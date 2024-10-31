package com.quincy.auth.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.quincy.auth.dao.LoginUserMappingRepository;
import com.quincy.auth.dao.UserRepository;
import com.quincy.auth.entity.LoginUserMappingEntity;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserService;
import com.quincy.core.dao.UtilsDao;
import com.quincy.sdk.Client;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.User;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private LoginUserMappingRepository loginUserMappingRepository;
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
		if(client.isMobile())
			user.setJsessionid(entity.getJsessionidMobileBrowser());
		if(client.isApp())
			user.setJsessionid(entity.getJsessionidPcBrowser());
		return user;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void add(UserEntity vo) {
		Long userId = vo.getId();
		if(userId==null) {
			userId = utilsDao.selectAutoIncreament("b_user");
			vo.setId(userId);
		}
		if(vo.getMobilePhone()!=null)
			this.createMapping(vo.getMobilePhone(), userId);
		if(vo.getEmail()!=null)
			this.createMapping(vo.getEmail(), userId);
		if(vo.getUsername()!=null)
			this.createMapping(vo.getUsername(), userId);
		userRepository.save(vo);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void createMapping(String loginName, Long userId) {
		LoginUserMappingEntity loginUserMappingEntity = new LoginUserMappingEntity();
		loginUserMappingEntity.setUserId(userId);
		loginUserMappingEntity.setLoginName(loginName);
		loginUserMappingRepository.save(loginUserMappingEntity);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Long createMapping(String loginName) {
		Long userId = utilsDao.selectAutoIncreament("b_user");
		this.createMapping(loginName, userId);
		return userId;
	}
}