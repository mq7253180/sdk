package com.quincy.core.aspect;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quincy.core.DTransactionConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.entity.Transaction;
import com.quincy.core.entity.TransactionArg;
import com.quincy.core.entity.TransactionAtomic;
import com.quincy.core.service.TransactionService;
import com.quincy.sdk.DTransactionOptRegistry;
import com.quincy.sdk.DTransactionFailure;
import com.quincy.sdk.annotation.transaction.AtomicOperational;
import com.quincy.sdk.annotation.transaction.DTransactional;
import com.quincy.sdk.annotation.transaction.ReferenceTo;
import com.quincy.sdk.helper.AopHelper;
import com.quincy.sdk.helper.CommonHelper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(7)
@Aspect
@Component
public class DistributedTransactionAop implements DTransactionOptRegistry {
	private final static ThreadLocal<List<TransactionAtomic>> atomicsHolder = new ThreadLocal<List<TransactionAtomic>>();
	private final static ThreadLocal<Boolean> inTransactionHolder = new ThreadLocal<Boolean>();
	private final static ThreadLocal<Boolean> inOrderHolder = new ThreadLocal<Boolean>();
	private final static int MSG_MAX_LENGTH = 200;

	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private TransactionService transactionService;
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_SYS_THREAD_POOL)
	private ThreadPoolExecutor threadPoolExecutor;
	@Value("${spring.application.name}")
	private String applicationName;

	@Pointcut("@annotation(com.quincy.sdk.annotation.transaction.DTransactional)")
    public void transactionPointCut() {}

	@Around("transactionPointCut()")
    public Object transactionAround(ProceedingJoinPoint joinPoint) throws Throwable {
		inTransactionHolder.set(true);
		atomicsHolder.set(new ArrayList<TransactionAtomic>());
		Class<?> clazz = joinPoint.getTarget().getClass();
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
		DTransactional annotation = method.getAnnotation(DTransactional.class);
		inOrderHolder.set(annotation.inOrder());
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
		tx.setBeanName(AopHelper.extractBeanName(clazz));
		tx.setMethodName(methodSignature.getName());
		tx.setParameterTypes(methodSignature.getParameterTypes());
		String flagForCronJob = CommonHelper.trim(annotation.flagForCronJob());
		if(flagForCronJob!=null)
			tx.setFlagForCronJob(flagForCronJob);
		tx.setInOrder(annotation.inOrder());
		final Transaction permanentTx = transactionService.insertTransaction(tx);
		atomics = permanentTx.getAtomics();
		if(atomics!=null&&atomics.size()>0)
			atomics.forEach(atomic->atomic.setMethodName(atomic.getConfirmMethodName()));
		boolean breakOnFailure = cancel?cancel:annotation.inOrder();
		this.invokeAtomicsAsExCaught(permanentTx, DTransactionConstants.ATOMIC_STATUS_SUCCESS, breakOnFailure);
		return retVal;
	}

	private void invokeAtomicsAsExCaught(Transaction tx, Integer statusTo, boolean breakOnFailure) {
		try {
			invokeAtomics(tx, DTransactionConstants.ATOMIC_STATUS_SUCCESS, breakOnFailure);
		} catch (Exception e) {
			log.error("\r\nDISTRIBUTED_TRANSACTION_ERR====================", e);
		}
	}

	@Pointcut("@annotation(com.quincy.sdk.annotation.transaction.AtomicOperational)")
    public void atomicOperationPointCut() {}

	@Around("atomicOperationPointCut()")
    public Object atomicOperationAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Boolean inTransaction = inTransactionHolder.get();
		if(inTransaction!=null&&inTransaction) {
			List<TransactionAtomic> atomics = atomicsHolder.get();
			Class<?> clazz = joinPoint.getTarget().getClass();
			MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
			Method method = clazz.getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
			AtomicOperational annotation = method.getDeclaredAnnotation(AtomicOperational.class);
			String confirmationMethodName = CommonHelper.trim(annotation.confirm());
			if(confirmationMethodName==null)
				throw new RuntimeException("Attribute 'confirm' must be specified a value.");
			Annotation[][] annotationss = method.getParameterAnnotations();
			Class<?>[] parameterTypes = methodSignature.getParameterTypes(); 
			String[] parameterTypeNames = new String[parameterTypes.length];
			for(int i=0;i<parameterTypes.length;i++) {
				boolean referency = false;
				int referenceTo = -1;
				Annotation[] annotations = annotationss[i];
				for(int j=0;j<annotations.length;j++) {
					Annotation a = annotations[j];
					if(ReferenceTo.class.getName().equals(a.annotationType().getName())) {
						if(inOrderHolder.get()==null||!inOrderHolder.get())
							throw new RuntimeException("The transaction must be in order if there is(are) parameter(s) reference to other method's return.");
						if(atomics.size()==0)
							throw new RuntimeException("The parameters of the first atomic operation cannot reference to others' return.");
						ReferenceTo r = (ReferenceTo)a;
						referenceTo = r.value();
						int previousAtomicIndex = atomics.size()-1;
						if(referenceTo<0||referenceTo>previousAtomicIndex) {
							throw new RuntimeException("The annotation ReferenceTo must be specified a value between 0 and "+previousAtomicIndex);
						} else {
							referency = true;
							break;
						}
					}
				}
				parameterTypeNames[i] = referency?DTransactionConstants.REFERENCE_TO+referenceTo:parameterTypes[i].getName();
			}
			String cancellationMethodName = CommonHelper.trim(annotation.cancel());
			String methodName = cancellationMethodName==null?confirmationMethodName:cancellationMethodName;
			TransactionAtomic atomic = new TransactionAtomic();
			atomic.setBeanName(AopHelper.extractBeanName(clazz));
			atomic.setMethodName(methodName);
			atomic.setConfirmMethodName(confirmationMethodName);
			atomic.setParameterTypes(parameterTypes);
			atomic.setParameterTypeNames(parameterTypeNames);
			atomic.setArgs(joinPoint.getArgs());
			atomic.setSort(atomics.size());
			atomic.setReturnType(method.getReturnType());
			atomic.setRetClass(method.getReturnType().getName());
			atomics.add(atomic);
		}
		return joinPoint.proceed();
	}

	@Override
	public void resume() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IOException, InterruptedException {
		this.resume(null);
	}

	@Override
	public void resume(String flagForCronJob) throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException {
		List<Transaction> failedTransactions = transactionService.findFailedTransactions(applicationName, CommonHelper.trim(flagForCronJob));
		this.compensate(failedTransactions);
	}

	private void compensate(List<Transaction> failedTransactions) throws ClassNotFoundException, NoSuchMethodException, IOException, InterruptedException {
		for(Transaction tx:failedTransactions) {
			int affected = transactionService.updateTransactionVersion(tx.getId(), tx.getVersion());
			if(affected>0) {//乐观锁, 集群部署多个结点时, 谁更新版本成功了谁负责执行
				log.warn("DISTRIBUTED_TRANSACTION_IS_EXECUTING===================={}", tx.getId());
				List<TransactionAtomic> atomics = transactionService.findTransactionAtomics(tx);
				tx.setAtomics(atomics);
				this.invokeAtomics(tx, tx.getType()==DTransactionConstants.TX_TYPE_CONFIRM?DTransactionConstants.ATOMIC_STATUS_SUCCESS:DTransactionConstants.ATOMIC_STATUS_CANCELED, tx.getInOrder());
			}
		}
	}

	private DTransactionFailure transactionFailure;
	private final static long ASYNC_WAIT_TIME_MILLIS = 50;
//	private final static int MAX_ASYNC_WAIT_TIMES = 200;

	@Data
	private class CountHolder {
		private int success;
		private int failure;
	}
	/**
	 * 调重试或撤消方法
	 * @param tx: 事务信息
	 * @param statusTo: 执行成功后要置的状态
	 * @param breakOnFailure: 其中一个失败后是否继续执行其他
	 */
	private void invokeAtomics(Transaction tx, Integer statusTo, boolean breakOnFailure) throws NoSuchMethodException, SecurityException, InterruptedException {
		List<TransactionAtomic> atomics = tx.getAtomics();
		List<TransactionAtomic> failureAtomics = new ArrayList<>(atomics.size());
		boolean success = false;
		if(atomics!=null&&atomics.size()>0) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			mapper.setSerializationInclusion(Include.NON_NULL);
			if(tx.getInOrder()||breakOnFailure) {
				for(int i=0;i<atomics.size();i++) {//逐个执行事务方法
					success = this.invokeAtomic(atomics, i, tx.getId(), statusTo, failureAtomics, mapper, true);
					if(!success&&breakOnFailure)
						break;
				}
			} else {
				CountHolder holder = new CountHolder();
				long start = System.currentTimeMillis();
				for(int i=0;i<atomics.size();i++)//多线程异步逐个执行事务方法
					this.asyncInvokeAtomic(atomics, i, tx.getId(), statusTo, failureAtomics, mapper, holder);
				int sleepTimes = 0;
				//sleepTimes<MAX_ASYNC_WAIT_TIMES&&
				while(holder.getSuccess()+holder.getFailure()<atomics.size()) {
					sleepTimes++;
					synchronized(holder) {//判断是否执行结束
						int total = holder.getSuccess()+holder.getFailure();
						if(total<atomics.size()) {
							holder.wait(ASYNC_WAIT_TIME_MILLIS);
							log.info("ASYNC_INVOCATION_SLEEPED-------------------{}--------{}", sleepTimes, total);
						} else
							break;
					}
				}
				log.info("ASYNC_DURATION=========================={}", (System.currentTimeMillis()-start));
				if(holder.getSuccess()==atomics.size()) {//全部成功
					success = true;
				}/* else if(atomicInvocationSuccess.get()+atomicInvocationFailure.get()<atomics.size()) {//还有线程没执行完
					
				}*/
			}
		}
		if(success) {
			transactionService.deleteTransaction(tx.getId());
		} else {
			Transaction txPo = this.updateTransactionToComleted(tx.getId());
			if(transactionFailure!=null) {
				Integer version = tx.getVersion();
				if(version==null)
					version = -1;
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				int retriesBeforeInform = transactionFailure.retriesBeforeInform();
				int retries = version+1;
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

	private void asyncInvokeAtomic(List<TransactionAtomic> atomics, int i, Long txId, Integer statusTo, List<TransactionAtomic> failureAtomics, ObjectMapper mapper, CountHolder holder) throws NoSuchMethodException {
		threadPoolExecutor.execute(()->{
			try {
				boolean atomicSuccess = this.invokeAtomic(atomics, i, txId, statusTo, failureAtomics, mapper, false);
				synchronized(holder) {
					if(atomicSuccess) {
						holder.setSuccess(holder.getSuccess()+1);
					} else
						holder.setFailure(holder.getFailure()+1);
					holder.notifyAll();
				}
			} catch (NoSuchMethodException e) {
				log.error("IMPOSSIBLE_EXCEPTION: ", e);
			}
		});
	}

	private boolean invokeAtomic(List<TransactionAtomic> atomics, int i, Long txId, Integer statusTo, List<TransactionAtomic> failureAtomics, ObjectMapper mapper, boolean sync) throws NoSuchMethodException {
		TransactionAtomic atomic = atomics.get(i);
		Object bean = applicationContext.getBean(atomic.getBeanName());
		TransactionAtomic toUpdate = new TransactionAtomic();
		toUpdate.setId(atomic.getId());
		Method method = null;
		try {
			method = bean.getClass().getMethod(atomic.getMethodName(), atomic.getParameterTypes());
		} catch(NoSuchMethodException e) {
			toUpdate.setMsg(CommonHelper.trim(e.toString()));
			transactionService.updateTransactionAtomic(toUpdate);
			this.updateTransactionToComleted(txId);
			throw e;
		}
		boolean update = true;
		boolean success = true;
		try {
			Object retVal = method.invoke(bean, atomic.getArgs());
			toUpdate.setStatus(statusTo);
			toUpdate.setRetValue(mapper.writeValueAsString(retVal));
			atomic.setRetValue(toUpdate.getRetValue());
			boolean referenced = false;
			for(int j=i+1;j<atomics.size();j++) {//在内存中更新后续输入参数依赖此操作输出的操作的输入参数
				TransactionAtomic atomicJ = atomics.get(j);
				for(int k = 0;k<atomicJ.getParameterTypeNames().length;k++) {
					String parameterTypeName = atomicJ.getParameterTypeNames()[k];
					if(parameterTypeName.startsWith(DTransactionConstants.REFERENCE_TO)) {
						int referenceTo = Integer.valueOf(parameterTypeName.substring(DTransactionConstants.REFERENCE_TO.length()));
						if(referenceTo==atomic.getSort()) {//此参数依赖当前方法的输出
							atomicJ.getParameterTypes()[k] = atomic.getReturnType();
							atomicJ.getParameterTypeNames()[k] = atomic.getRetClass();
							atomicJ.getArgs()[k] = retVal;
							referenced = true;
						}
					}
				}
			}
			if(referenced) {
				int result = transactionService.updateTransactionAtomicArgs(atomic.getTxId(), DTransactionConstants.REFERENCE_TO+atomic.getSort(), atomic.getRetClass(), atomic.getRetValue());
				if(result==0)
					throw new RuntimeException("SDK bug occurred!----------"+atomic.getId()+"----------"+DTransactionConstants.REFERENCE_TO+atomic.getSort());
			}
		} catch(Exception e) {
			success = false;
			failureAtomics.add(atomic);
			Throwable cause = e.getCause()==null?e:e.getCause();
			String msg = CommonHelper.trim(cause.toString());
			if(msg!=null) {
				if(msg.length()>MSG_MAX_LENGTH)
					msg = msg.substring(0, MSG_MAX_LENGTH);
				toUpdate.setMsg(msg);
				atomic.setMsg(msg);
			} else
				update = false;
			log.error("\r\nDISTRIBUTED_TRANSACTION_ERR====================", cause);
		} finally {
			if(update)
				transactionService.updateTransactionAtomic(toUpdate);
		}
		return success;
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