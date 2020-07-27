package com.quincy.core.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.core.dao.TransactionAtomicRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.quincy.core.DTransactionConstants;
import com.quincy.core.dao.TransactionArgRepository;
import com.quincy.core.dao.TransactionRepository;
import com.quincy.core.entity.Transaction;
import com.quincy.core.entity.TransactionArg;
import com.quincy.core.entity.TransactionAtomic;
import com.quincy.core.mapper.CoreMapper;
import com.quincy.core.service.TransactionService;
import com.quincy.sdk.helper.CommonHelper;

@Service
public class TransactionServiceImpl implements TransactionService {
	private final static Map<String, Class<?>> clazzMap = new HashMap<String, Class<?>>(8);
	static {
		clazzMap.put("byte", byte.class);
		clazzMap.put("short", short.class);
		clazzMap.put("int", int.class);
		clazzMap.put("long", long.class);
		clazzMap.put("float", float.class);
		clazzMap.put("double", double.class);
		clazzMap.put("char", char.class);
		clazzMap.put("boolean", boolean.class);
	}

	@Autowired
	private TransactionRepository transactionRepository;
	@Autowired
	private TransactionAtomicRepository transactionAtomicRepository;
	@Autowired
	private TransactionArgRepository transactionArgRepository;
	@Autowired
	private CoreMapper coreMapper;

	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	@Override
	public Transaction insertTransaction(Transaction _tx) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		Object[] args = _tx.getArgs();
		Transaction tx = transactionRepository.save(_tx);
		tx.setArgs(args);
		this.saveArgs(args, _tx.getParameterTypes(), tx.getId(), DTransactionConstants.ARG_TYPE_TX, mapper);
		List<TransactionAtomic> _atomics = _tx.getAtomics();
		if(_atomics!=null&&_atomics.size()>0) {
			List<TransactionAtomic> atomics = new ArrayList<TransactionAtomic>(_atomics.size());
			for(TransactionAtomic _atomic:_atomics) {
				args = _atomic.getArgs();
				_atomic.setTxId(tx.getId());
				TransactionAtomic atomic = transactionAtomicRepository.save(_atomic);
				atomic.setArgs(args);
				atomic.setArgList(this.saveArgs(args, _atomic.getParameterTypes(), atomic.getId(), DTransactionConstants.ARG_TYPE_ATOMIC, mapper));
				atomics.add(atomic);
			}
			tx.setAtomics(atomics);
		}
		return tx;
	}

	private List<TransactionArg> saveArgs(Object[] _args, Class<?>[] parameterTypes, Long parentId, int type, ObjectMapper mapper) throws JsonProcessingException {
		List<TransactionArg> args = null;
		if(_args!=null&&_args.length>0) {
			args = new ArrayList<TransactionArg>(_args.length);
			for(int i=0;i<_args.length;i++) {
				Object _arg = _args[i];
				TransactionArg arg = new TransactionArg();
				arg.setParentId(parentId);
				arg.setClazz(parameterTypes[i].getName());
				arg.setValue(mapper.writeValueAsString(_arg));
				arg.setSort(i);
				arg.setType(type);
				arg = transactionArgRepository.save(arg);
				args.add(arg);
			}
		}
		return args;
	}

	@Override
	public TransactionAtomic updateTransactionAtomic(TransactionAtomic _atomic) {
		TransactionAtomic atomic = null;
		if(_atomic!=null&&_atomic.getId()!=null) {
			Optional<TransactionAtomic> o = transactionAtomicRepository.findById(_atomic.getId());
			if(o.isPresent()) {
				atomic = o.get();
				if(_atomic.getStatus()!=null)
					atomic.setStatus(_atomic.getStatus());
				if(_atomic.getMsg()!=null)
					atomic.setMsg(CommonHelper.trim(_atomic.getMsg()));
				atomic = transactionAtomicRepository.save(atomic);
			}
		}
		return atomic;
	}

	@Override
	public Transaction updateTransaction(Transaction _tx) {
		Transaction tx = null;
		if(_tx!=null&&_tx.getId()!=null) {
			Optional<Transaction> o = transactionRepository.findById(_tx.getId());
			if(o.isPresent()) {
				tx = o.get();
				if(_tx.getStatus()!=null)
					tx.setStatus(_tx.getStatus());
				if(_tx.getLastExecuted()!=null)
					tx.setLastExecuted(_tx.getLastExecuted());;
				tx = transactionRepository.save(tx);
			}
		}
		return tx;
	}

	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	@Override
	public void deleteTransaction(Long id) {
		coreMapper.deleteTransactionAtomicArgs(id);
		coreMapper.deleteTransactionAtomics(id);
		coreMapper.deleteArgs(id, DTransactionConstants.ARG_TYPE_TX);
		coreMapper.deleteTransaction(id);
	}

	@Override
	public List<Transaction> findFailedTransactions(String applicationName, String frequencyBatch) {
		return frequencyBatch==null?transactionRepository.findByApplicationNameAndStatus(applicationName, DTransactionConstants.TX_STATUS_ED):transactionRepository.findByApplicationNameAndStatusAndFrequencyBatch(applicationName, DTransactionConstants.TX_STATUS_ED, frequencyBatch);
	}

	@Override
	public int updateTransactionVersion(Long id, Integer version) {
		return coreMapper.updateTransactionVersion(id, version);
	}

	@Override
	public List<TransactionAtomic> findTransactionAtomics(Transaction tx) throws ClassNotFoundException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<TransactionAtomic> atomics =  (tx.getInOrder()&&tx.getType().intValue()==DTransactionConstants.TX_TYPE_CANCEL)?transactionAtomicRepository.findByTxIdAndStatusOrderBySortDesc(tx.getId(), tx.getType()):transactionAtomicRepository.findByTxIdAndStatusOrderBySort(tx.getId(), tx.getType());
		if(atomics!=null&&atomics.size()>0) {
			for(TransactionAtomic atomic:atomics) {
				List<TransactionArg> _args = transactionArgRepository.findByParentIdAndTypeOrderBySort(atomic.getId(), DTransactionConstants.ARG_TYPE_ATOMIC);
				if(_args!=null&&_args.size()>0) {
					Class<?>[] parameterTypes = new Class<?>[_args.size()];
					Object[] args = new Object[_args.size()];
					for(int i=0;i<_args.size();i++) {
						TransactionArg _arg = _args.get(i);
						Class<?> parameterType = clazzMap.get(_arg.getClazz());
						if(parameterType==null)
							parameterType = Class.forName(_arg.getClazz(), false, this.getClass().getClassLoader());
						parameterTypes[i] = parameterType;
						Object arg = mapper.readValue(_arg.getValue(), parameterType);
						args[i] = arg;
					}
					atomic.setParameterTypes(parameterTypes);
					atomic.setArgs(args);
					atomic.setArgList(_args);
				}
			}
		}
		return atomics;
	}

	@Override
	public List<TransactionArg> findArgs(Long parentId, Integer type) {
		return transactionArgRepository.findByParentIdAndTypeOrderBySort(parentId, type);
	}
}