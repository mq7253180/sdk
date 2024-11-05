package com.quincy.auth.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.quincy.auth.dao.LoginUserMappingRepository;
import com.quincy.auth.dao.UserRepository;
import com.quincy.auth.entity.LoginUserMappingEntity;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.UserUpdation;
import com.quincy.core.dao.UtilsDao;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.User;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	protected LoginUserMappingRepository loginUserMappingRepository;
	@Autowired
	protected UserRepository userRepository;
	@Autowired
	private UtilsDao utilsDao;

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public UserEntity update(UserEntity vo) {
		UserEntity po = userRepository.findById(vo.getId()).get();
		String username = CommonHelper.trim(vo.getUsername());
		if(username!=null)
			po.setUsername(username);
		String password = CommonHelper.trim(vo.getPassword());
		if(password!=null)
			po.setPassword(password);
		String email = CommonHelper.trim(vo.getEmail());
		if(email!=null)
			po.setEmail(email);
		String mobilePhone = CommonHelper.trim(vo.getMobilePhone());
		if(mobilePhone!=null)
			po.setMobilePhone(mobilePhone);
		String name = CommonHelper.trim(vo.getName());
		if(name!=null)
			po.setName(name);
		Byte gender = vo.getGender();
		if(gender!=null)
			po.setGender(gender);
		String avatar = CommonHelper.trim(vo.getAvatar());
		if(avatar!=null)
			po.setAvatar(avatar);
		String jsessionidPcBrowser = CommonHelper.trim(vo.getJsessionidPcBrowser());
		if(jsessionidPcBrowser!=null)
			po.setJsessionidPcBrowser(jsessionidPcBrowser);
		String jsessionidMobileBrowser = CommonHelper.trim(vo.getJsessionidMobileBrowser());
		if(jsessionidMobileBrowser!=null)
			po.setJsessionidMobileBrowser(jsessionidMobileBrowser);
		String jsessionidApp = CommonHelper.trim(vo.getJsessionidApp());
		if(jsessionidApp!=null)
			po.setJsessionidApp(jsessionidApp);
		userRepository.save(po);
		return po;
	}

	@Override
	public Long findUserId(String loginName) {
		LoginUserMappingEntity loginUserMappingEntity = loginUserMappingRepository.findByLoginName(loginName);
		return loginUserMappingEntity==null?null:loginUserMappingEntity.getUserId();
	}

	@Override
	@ReadOnly
	public User find(Long id, Client client) {
		Optional<UserEntity> optional = userRepository.findById(id);
		if(optional.isPresent()) {
			UserEntity entity = optional.get();
			return this.toUser(entity, client);
		} else
			return null;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updatePassword(Long userId, String password) {
		UserEntity vo = new UserEntity();
		vo.setId(userId);
		vo.setPassword(password);
		this.update(vo);
	}

	protected User toUser(UserEntity entity, Client client) {
		User user = new User();
		user.setId(entity.getId());
		user.setCreationTime(entity.getCreationTime());
		user.setName(entity.getName());
		user.setUsername(entity.getUsername());
		user.setMobilePhone(entity.getMobilePhone());
		user.setEmail(entity.getEmail());
		user.setPassword(entity.getPassword());
		user.setGender(entity.getGender());
		user.setAvatar(entity.getAvatar());
		if(client.isPc())
			user.setJsessionid(entity.getJsessionidPcBrowser());
		else if(client.isMobile())
			user.setJsessionid(entity.getJsessionidMobileBrowser());
		else if(client.isApp())
			user.setJsessionid(entity.getJsessionidPcBrowser());
		return user;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Long add(UserEntity vo) {
		UserEntity po = userRepository.save(vo);
		return po.getId();
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public boolean createMapping(String loginName, Long userId) {
		LoginUserMappingEntity po = loginUserMappingRepository.findByLoginName(loginName);
		if(po!=null) {
			return false;
		} else {
			this.doCreateMapping(loginName, userId);
			return true;
		}
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Long createMapping(String loginName) {
		LoginUserMappingEntity po = loginUserMappingRepository.findByLoginName(loginName);
		if(po!=null) {
			return null;
		} else {
			Long userId = utilsDao.selectAutoIncreament("b_user");
			this.doCreateMapping(loginName, userId);
			return userId;
		}
	}

	private void doCreateMapping(String loginName, Long userId) {
		LoginUserMappingEntity loginUserMappingEntity = new LoginUserMappingEntity();
		loginUserMappingEntity.setUserId(userId);
		loginUserMappingEntity.setLoginName(loginName);
		loginUserMappingRepository.save(loginUserMappingEntity);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Result updateMapping(String oldLoginName, String newLoginName, UserUpdation userUpdation) {
		LoginUserMappingEntity po = loginUserMappingRepository.findByLoginName(newLoginName);
		if(po!=null)//验重
			return new Result(0, "auth.mapping.new");
		po = loginUserMappingRepository.findByLoginName(oldLoginName);
		Assert.notNull(po, "开发错误：旧手机号、邮箱、用户名不存在，请检查！");
		po.setLoginName(newLoginName);
		loginUserMappingRepository.save(po);
		UserEntity vo = new UserEntity();
		vo.setId(po.getUserId());
		userUpdation.setLoginName(vo);
		this.userRepository.save(vo);
		return new Result(1, "status.success");
	}

	@Override
	public void deleteMappingAndUpdateUser(String oldLoginName, UserUpdation userUpdation, Long userId) {
		throw new RuntimeException("此方法专门提供给分片库模式，单库模式下禁调此方法！");
	}
}