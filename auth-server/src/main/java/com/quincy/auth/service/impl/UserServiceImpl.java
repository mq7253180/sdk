package com.quincy.auth.service.impl;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quincy.auth.UserBase;
import com.quincy.auth.dao.UserRepository;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserService;
import com.quincy.sdk.Client;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.User;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private UserRepository userRepository;

	@Transactional
	@Override
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
	@ReadOnly
	public User find(String loginName, Client client) {
		UserEntity entity = userRepository.findByUsernameOrEmailOrMobilePhone(loginName, loginName, loginName);
		return entity==null?null:this.toUser(entity, client);
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
	public void updatePassword(Long userId, String password) {
		UserEntity vo = new UserEntity();
		vo.setId(userId);
		vo.setPassword(password);
		this.update(vo);
	}

	protected User toUser(UserBase entity, Client client) {
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
}