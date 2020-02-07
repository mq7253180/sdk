package com.quincy.core.aspect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.quincy.core.TransactionConstants;
import com.quincy.core.entity.Transaction;
import com.quincy.core.entity.TransactionAtomic;
import com.quincy.core.service.TransactionService;
import com.quincy.sdk.annotation.transaction.AtomicOperational;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(7)
@Component
public class DistributedTransactionAop {
	private final static ThreadLocal<List<TransactionAtomic>> atomicsHolder = new ThreadLocal<List<TransactionAtomic>>();
	private final static ThreadLocal<Boolean> inTransactionHolder = new ThreadLocal<Boolean>();
	private final static int MSG_MAX_LENGTH = 200;

	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private TransactionService transactionService;

	@Pointcut("@annotation(com.quincy.sdk.annotation.transaction.DistributedTransactional)")
    public void transactionPointCut() {}

	@Around("transactionPointCut()")
    public Object transactionAround(ProceedingJoinPoint joinPoint) throws Throwable {
		inTransactionHolder.set(true);
		atomicsHolder.set(new ArrayList<TransactionAtomic>());
		Object retVal = joinPoint.proceed();
		List<TransactionAtomic> atomics = atomicsHolder.get();
		boolean cancel = false;
		for(TransactionAtomic atomic:atomics) {
			if(!atomic.getMethodName().equals(atomic.getConfirmMethodName())) {
				cancel = true;
				break;
			}
		}
		for(TransactionAtomic atomic:atomics) {
			if(cancel&&atomic.getMethodName().equals(atomic.getConfirmMethodName()))
				throw new RuntimeException("In a same transaction scpoe, all of attributes of 'cancel' must be specified a value if there are atomic operation(s) are specified by attribute 'cancel'.");
		}
		Transaction tx = new Transaction();
		tx.setAtomics(atomics);
		tx.setType(cancel?TransactionConstants.TX_TYPE_CANCEL:TransactionConstants.TX_TYPE_CONFIRM);
		tx.setArgs(joinPoint.getArgs());
		Class<?> clazz = joinPoint.getTarget().getClass();
		tx.setBeanName(this.getBeanName(clazz));
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		tx.setMethodName(methodSignature.getName());
		tx.setParameterTypes(methodSignature.getParameterTypes());
		tx = transactionService.insertTransaction(tx);
		atomics = tx.getAtomics();
		if(atomics!=null&&atomics.size()>0) {
			for(TransactionAtomic atomic:atomics)
				atomic.setMethodName(atomic.getConfirmMethodName());
		}
		this.invokeAtomics(tx, TransactionConstants.ATOMIC_STATUS_SUCCESS, cancel);
		return retVal;
	}

	@Pointcut("@annotation(com.quincy.sdk.annotation.transaction.AtomicOperational)")
    public void atomicOperationPointCut() {}

	@Around("atomicOperationPointCut()")
    public Object atomicOperationAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Boolean inTransaction = inTransactionHolder.get();
		if(inTransaction!=null&&inTransaction) {
			Class<?> clazz = joinPoint.getTarget().getClass();
			MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
			Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
			AtomicOperational annotation = method.getDeclaredAnnotation(AtomicOperational.class);
			String confirmationMethodName = CommonHelper.trim(annotation.confirm());
			if(confirmationMethodName==null)
				throw new RuntimeException("Attribute 'confirm' must be specified a value.");
			String cancellationMethodName = CommonHelper.trim(annotation.cancel());
			String methodName = cancellationMethodName==null?confirmationMethodName:cancellationMethodName;
			TransactionAtomic atomic = new TransactionAtomic();
			atomic.setBeanName(this.getBeanName(clazz));
			atomic.setMethodName(methodName);
			atomic.setConfirmMethodName(confirmationMethodName);
			atomic.setParameterTypes(methodSignature.getParameterTypes());
			atomic.setArgs(joinPoint.getArgs());
			List<TransactionAtomic> atomics = atomicsHolder.get();
			atomic.setSort(atomics.size());
			atomics.add(atomic);
		}
		return null;
	}

	@Scheduled(cron = "0 0/1 * * * ?")
	public void compensate() throws Exception {
		List<Transaction> failedTransactions = transactionService.findFailedTransactions();
		for(Transaction tx:failedTransactions) {
			int affected = transactionService.updateTransactionVersion(tx.getId(), tx.getVersion());
			if(affected>0) {//乐观锁, 集群部署多个结点时, 谁更新版本成功了谁负责执行
				List<TransactionAtomic> atomics = transactionService.findTransactionAtomics(tx.getId(), tx.getType());
				tx.setAtomics(atomics);
				if(tx.getType()==TransactionConstants.TX_TYPE_CONFIRM)
					this.invokeAtomics(tx, TransactionConstants.ATOMIC_STATUS_SUCCESS, false);
				else
					this.invokeAtomics(tx, TransactionConstants.ATOMIC_STATUS_CANCELED, false);
			}
		}
	}

	private String getBeanName(Class<?> clazz) {
		String beanName = CommonHelper.trim(chainHead.support(clazz));
		if(beanName==null) {
			String className = clazz.getName();
			int lastDotIndex = className.lastIndexOf(".");
			int lastDotIndexPlus2 = lastDotIndex+2;
			beanName = className.substring(lastDotIndexPlus2, className.length());
			beanName = className.substring(lastDotIndex+1, lastDotIndexPlus2).toLowerCase()+beanName;
		}
		return beanName;
	}

	/**
	 * 调重试或撤消方法
	 * @param tx: 事务信息
	 * @param statusTo: 执行成功后要置的状态
	 * @param breakOnFailure: 其中一个失败后是否继续执行其他
	 */
	private void invokeAtomics(Transaction tx, Integer statusTo, boolean breakOnFailure) throws NoSuchMethodException, SecurityException {
		List<TransactionAtomic> atomics = tx.getAtomics();
		boolean success = true;
		if(atomics!=null&&atomics.size()>0) {
			for(TransactionAtomic atomic:atomics) {//逐个执行事务方法
				Object bean = applicationContext.getBean(atomic.getBeanName());
				Method method = null;
				try {
					log.info("===================={}", atomic.getBeanName()+"."+atomic.getMethodName());
					for(Class<?> clazz:atomic.getParameterTypes())
						log.info("-----------{}", clazz.getName());
					method = bean.getClass().getMethod(atomic.getMethodName(), atomic.getParameterTypes());
				} catch(NoSuchMethodException e) {
					this.updateTransactionToComleted(tx.getId());
					throw e;
				}
				TransactionAtomic toUpdate = new TransactionAtomic();
				toUpdate.setId(atomic.getId());
				boolean update = true;
				try {
					method.invoke(bean, atomic.getArgs());
					toUpdate.setStatus(statusTo);
				} catch(Exception e) {
					success = false;
					String msg = CommonHelper.trim(e.getCause().toString());
					if(msg!=null) {
						if(msg.length()>MSG_MAX_LENGTH)
							msg = msg.substring(0, MSG_MAX_LENGTH);
						toUpdate.setMsg(msg);
					} else
						update = false;
					log.error("DISTRIBUTED_TRANSACTION_ERR====================", e);
					if(breakOnFailure)
						break;
				} finally {
					if(update)
						transactionService.updateTransactionAtomic(toUpdate);
				}
			}
		}
		if(success) {
			transactionService.deleteTransaction(tx.getId());
		} else
			this.updateTransactionToComleted(tx.getId());
	}

	private void updateTransactionToComleted(Long id) {
		Transaction toUpdate = new Transaction();
		toUpdate.setId(id);
		toUpdate.setStatus(TransactionConstants.TX_STATUS_ED);
		toUpdate.setLastExecuted(new Date());
		transactionService.updateTransaction(toUpdate);
	}

	private static Support chainHead;

	static {
		Support serviceSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Service annotation = clazz.getDeclaredAnnotation(Service.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support componentSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Component annotation = clazz.getDeclaredAnnotation(Component.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support controllerSupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Controller annotation = clazz.getDeclaredAnnotation(Controller.class);
				return annotation==null?null:annotation.value();
			}
		};
		Support repositorySupport = new Support() {
			@Override
			protected String resolve(Class<?> clazz) {
				Repository annotation = clazz.getDeclaredAnnotation(Repository.class);
				return annotation==null?null:annotation.value();
			}
		};
		serviceSupport.setNext(componentSupport).setNext(controllerSupport).setNext(repositorySupport);
		chainHead = serviceSupport;
	}

	private static abstract class Support {
		private Support next;

		protected abstract String resolve(Class<?> clazz);

		public Support setNext(Support next) {
			this.next = next;
			return next;
		}

		public String support(Class<?> clazz) {
			String beanName = this.resolve(clazz);
			return beanName==null?(this.next==null?null:this.next.support(clazz)):beanName;
		}
	}
}