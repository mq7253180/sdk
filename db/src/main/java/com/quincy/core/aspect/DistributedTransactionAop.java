package com.quincy.core.aspect;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Resource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.quincy.core.DTransactionConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.entity.Transaction;
import com.quincy.core.entity.TransactionArg;
import com.quincy.core.entity.TransactionAtomic;
import com.quincy.core.service.TransactionService;
import com.quincy.sdk.DTransactionContext;
import com.quincy.sdk.DTransactionFailure;
import com.quincy.sdk.annotation.transaction.AtomicOperational;
import com.quincy.sdk.annotation.transaction.DTransactional;
import com.quincy.sdk.helper.AopHelper;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(7)
@Component
public class DistributedTransactionAop implements DTransactionContext {
	private final static ThreadLocal<List<TransactionAtomic>> atomicsHolder = new ThreadLocal<List<TransactionAtomic>>();
	private final static ThreadLocal<Boolean> inTransactionHolder = new ThreadLocal<Boolean>();
	private final static int MSG_MAX_LENGTH = 200;

	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private TransactionService transactionService;
	@Resource(name = InnerConstants.BEAN_NAME_SYS_THREAD_POOL)
	private ThreadPoolExecutor threadPoolExecutor;
	@Value("${spring.application.name}")
	private String applicationName;

	@Pointcut("@annotation(com.quincy.sdk.annotation.transaction.DTransactional)")
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
		tx.setApplicationName(applicationName);
		tx.setAtomics(atomics);
		tx.setType(cancel?DTransactionConstants.TX_TYPE_CANCEL:DTransactionConstants.TX_TYPE_CONFIRM);
		tx.setArgs(joinPoint.getArgs());
		Class<?> clazz = joinPoint.getTarget().getClass();
		tx.setBeanName(AopHelper.extractBeanName(clazz));
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		tx.setMethodName(methodSignature.getName());
		tx.setParameterTypes(methodSignature.getParameterTypes());
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		DTransactional annotation = method.getAnnotation(DTransactional.class);
		String frequencyBatch = CommonHelper.trim(annotation.frequencyBatch());
		if(frequencyBatch!=null)
			tx.setFrequencyBatch(frequencyBatch);
		tx.setInOrder(annotation.inOrder());
		final Transaction permanentTx = transactionService.insertTransaction(tx);
		atomics = permanentTx.getAtomics();
		if(atomics!=null&&atomics.size()>0) {
			for(TransactionAtomic atomic:atomics)
				atomic.setMethodName(atomic.getConfirmMethodName());
		}
		boolean breakOnFailure = cancel?cancel:annotation.inOrder();
		if(annotation.async()) {
			String executorBeanName = CommonHelper.trim(annotation.executor());
			Executor executor = executorBeanName==null?threadPoolExecutor:(Executor)applicationContext.getBean(executorBeanName);
			executor.execute(new Runnable() {
				@Override
				public void run() {
					invokeAtomicsAsExCaught(permanentTx, DTransactionConstants.ATOMIC_STATUS_SUCCESS, breakOnFailure);
				}
			});
		} else
			this.invokeAtomicsAsExCaught(permanentTx, DTransactionConstants.ATOMIC_STATUS_SUCCESS, breakOnFailure);
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
			atomic.setBeanName(AopHelper.extractBeanName(clazz));
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

//	@Scheduled(cron = "0 0/1 * * * ?")
	@Override
	public void compensate() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException {
		List<Transaction> failedTransactions = transactionService.findFailedTransactions(applicationName, null);
		this.compensate(failedTransactions);
	}

	@Override
	public void compensate(String frequencyBatch) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException {
		List<Transaction> failedTransactions = transactionService.findFailedTransactions(applicationName, CommonHelper.trim(frequencyBatch));
		this.compensate(failedTransactions);
	}

	private void compensate(List<Transaction> failedTransactions) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException {
		for(Transaction tx:failedTransactions) {
			Object bean = applicationContext.getBean(tx.getBeanName());
			if(bean!=null) {
				int affected = transactionService.updateTransactionVersion(tx.getId(), tx.getVersion());
				if(affected>0) {//乐观锁, 集群部署多个结点时, 谁更新版本成功了谁负责执行
					log.warn("DISTRIBUTED_TRANSACTION_IS_EXECUTING===================={}", tx.getId());
					List<TransactionAtomic> atomics = transactionService.findTransactionAtomics(tx);
					tx.setAtomics(atomics);
					this.invokeAtomics(tx, tx.getType()==DTransactionConstants.TX_TYPE_CONFIRM?DTransactionConstants.ATOMIC_STATUS_SUCCESS:DTransactionConstants.ATOMIC_STATUS_CANCELED, tx.getInOrder());
				}
			}
		}
	}

	private DTransactionFailure transactionFailure;

	/**
	 * 调重试或撤消方法
	 * @param tx: 事务信息
	 * @param statusTo: 执行成功后要置的状态
	 * @param breakOnFailure: 其中一个失败后是否继续执行其他
	 */
	private void invokeAtomics(Transaction tx, Integer statusTo, boolean breakOnFailure) throws NoSuchMethodException, SecurityException {
		List<TransactionAtomic> atomics = tx.getAtomics();
		List<TransactionAtomic> failureAtomics = new ArrayList<>(atomics.size());
		boolean success = true;
		if(atomics!=null&&atomics.size()>0) {
			for(TransactionAtomic atomic:atomics) {//逐个执行事务方法
				Object bean = applicationContext.getBean(atomic.getBeanName());
				TransactionAtomic toUpdate = new TransactionAtomic();
				toUpdate.setId(atomic.getId());
				Method method = null;
				try {
					method = bean.getClass().getMethod(atomic.getMethodName(), atomic.getParameterTypes());
				} catch(NoSuchMethodException e) {
					toUpdate.setMsg(CommonHelper.trim(e.toString()));
					transactionService.updateTransactionAtomic(toUpdate);
					this.updateTransactionToComleted(tx.getId());
					throw e;
				}
				boolean update = true;
				try {
					method.invoke(bean, atomic.getArgs());
					toUpdate.setStatus(statusTo);
					toUpdate.setMsg("");
				} catch(Exception e) {
					success = false;
					failureAtomics.add(atomic);
					Throwable cause = e.getCause();
					String msg = CommonHelper.trim(cause.toString());
					if(msg!=null) {
						if(msg.length()>MSG_MAX_LENGTH)
							msg = msg.substring(0, MSG_MAX_LENGTH);
						toUpdate.setMsg(msg);
						atomic.setMsg(msg);
					} else
						update = false;
					log.error("\r\nDISTRIBUTED_TRANSACTION_ERR====================", cause);
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
		} else {
			Transaction txPo = this.updateTransactionToComleted(tx.getId());
			if(transactionFailure!=null) {
				if(tx.getVersion()==null)
					tx.setVersion(-1);
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				int retriesBeforeInform = transactionFailure.retriesBeforeInform();
				int retries = tx.getVersion()+1;
				if(retries>=retriesBeforeInform) {
					List<TransactionArg> args = transactionService.findArgs(tx.getId(), DTransactionConstants.ARG_TYPE_TX);
					StringBuilder message = new StringBuilder(350).append(tx.getApplicationName()).append(".");
					this.appendMethodAndArgs(message, tx.getBeanName(), tx.getMethodName(), args)
					.append("\r\n创建时间: ").append(tx.getCreationTime()==null?"":df.format(tx.getCreationTime()))
					.append("\r\n最后执行时间: ").append(txPo.getLastExecuted()==null?"":df.format(txPo.getLastExecuted()))
					.append("\r\n已执行了: ").append(retries).append("次");
					for(TransactionAtomic atomic:failureAtomics) {
						args = atomic.getArgList();
						message.append("\r\n\t");
						this.appendMethodAndArgs(message, atomic.getBeanName(), atomic.getMethodName(), args)
						.append(": ").append(atomic.getMsg());
					}
					threadPoolExecutor.execute(new Runnable() {
						@Override
						public void run() {
							transactionFailure.inform(message.toString());
						}
					});
				}
			}
		}
	}

	private void invokeAtomicsAsExCaught(Transaction tx, Integer statusTo, boolean breakOnFailure) {
		try {
			invokeAtomics(tx, DTransactionConstants.ATOMIC_STATUS_SUCCESS, breakOnFailure);
		} catch (Exception e) {
			log.error("\r\nDISTRIBUTED_TRANSACTION_ERR====================", e);
		}
	}

	private Transaction updateTransactionToComleted(Long id) {
		Transaction toUpdate = new Transaction();
		toUpdate.setId(id);
		toUpdate.setStatus(DTransactionConstants.TX_STATUS_ED);
		toUpdate.setLastExecuted(new Date());
		Transaction tx = transactionService.updateTransaction(toUpdate);
		return tx;
	}

	private StringBuilder appendMethodAndArgs(StringBuilder message, String beanName, String methodName, List<TransactionArg> args) {
		if(args==null)
			args = new ArrayList<TransactionArg>(0);
		message.append(beanName).append(".").append(methodName).append("(");
		int appendComma = args.size()-1;
		for(int i=0;i<args.size();i++) {
			TransactionArg arg = args.get(i);
			message.append(arg.getValue());
			if(i<appendComma)
				message.append(", ");
		}
		return message.append(")");
	}

	@Override
	public void setTransactionFailure(DTransactionFailure transactionFailure) {
		this.transactionFailure = transactionFailure;
	}
}