package com.quincy.auth.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.quincy.auth.dao.LoginUserMappingDao;
import com.quincy.auth.dao.UserDao;
import com.quincy.auth.entity.LoginUserMapping;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.entity.UserDto;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.UserUpdation;
import com.quincy.core.dao.UtilsDao;
import com.quincy.sdk.AuthServerActions;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.Menu;
import com.quincy.sdk.o.User;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	protected LoginUserMappingDao loginUserMappingDao;
	@Autowired
	private UtilsDao utilsDao;
	@Autowired
	protected UserDao userDao;
	@Autowired
	private AuthServerActions authServerActions;

	@ReadOnly
	@Override
	public void loadAuth(User user) {
		//角色
		List<Role> roleList = authServerActions.findRoles(user.getId());
		if(roleList!=null&&roleList.size()>0) {
			Map<Long, String> roleMap = new HashMap<Long, String>(roleList.size());
			for(Role role:roleList)//去重
				roleMap.put(role.getId(), role.getName());
			List<String> roles = new ArrayList<String>(roleMap.size());
			roles.addAll(roleMap.values());
			user.setRoles(roles);
		}
		//权限
		List<Permission> permissionList = authServerActions.findPermissions(user.getId());
		if(permissionList!=null&&permissionList.size()>0) {
			Map<Long, String> permissionMap = new HashMap<Long, String>(permissionList.size());
			for(Permission permission:permissionList)//去重
				permissionMap.put(permission.getId(), permission.getName());
			List<String> permissions = new ArrayList<String>(permissionMap.size());
			permissions.addAll(permissionMap.values());
			user.setPermissions(permissions);
		}
		//菜单
		List<Menu> rootMenus = this.findMenusByUserIdAndEnterpriseId(user.getId());
		if(rootMenus!=null&&rootMenus.size()>0)
			user.setMenus(rootMenus);
	}

	private List<Menu> findMenusByUserIdAndEnterpriseId(Long userId) {
		List<Menu> allMenus = authServerActions.findMenus(userId);
		if(allMenus!=null&&allMenus.size()>0) {
			Map<Long, Menu> duplicateRemovedMenus = new HashMap<Long, Menu>(allMenus.size());
			for(Menu menu:allMenus)
				duplicateRemovedMenus.put(menu.getId(), menu);
			List<Menu> rootMenus = new ArrayList<Menu>(duplicateRemovedMenus.size());
			Set<Entry<Long, Menu>> entrySet = duplicateRemovedMenus.entrySet();
			for(Entry<Long, Menu> entry:entrySet) {
				Menu menu = entry.getValue();
				if(menu.getPId()==null) {
					rootMenus.add(menu);
					this.loadChildrenMenus(menu, entrySet);
				}
			}
			return rootMenus;
		} 
		return null;
	}

	private void loadChildrenMenus(Menu parent, Set<Entry<Long, Menu>> entrySet) {
		for(Entry<Long, Menu> entry:entrySet) {
			Menu menu = entry.getValue();
			if(parent.getId()==menu.getPId()) {
				if(parent.getChildren()==null)
					parent.setChildren(new ArrayList<Menu>(10));
				parent.getChildren().add(menu);
			}
		}
		if(parent.getChildren()!=null&&parent.getChildren().size()>0) {
			for(Menu child:parent.getChildren())
				this.loadChildrenMenus(child, entrySet);
		}
	}
	/*
	@Override
	public void updateSession(User user) {
		String jsessionid = CommonHelper.trim(user.getJsessionid());
		if(jsessionid!=null) {
			HttpSession session = AuthSessionHolder.SESSIONS.get(jsessionid);
			XSession xsession = this.createSession(user);
			session.setAttribute(InnerConstants.ATTR_SESSION, xsession);
		}
	}

	@Override
	public void updateSession(List<User> users) throws IOException {
		for(User user:users)
			this.updateSession(user);
	}
	*/

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public UserDto update(UserDto vo) {
		UserDto po = userDao.find(vo.getId());
//		UserEntity po = userRepository.findById(vo.getId()).get();
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
//		userRepository.save(po);
		userDao.update(po.getName(), po.getUsername(), po.getGender(), po.getPassword(), po.getMobilePhone(), po.getEmail(), po.getAvatar(), po.getJsessionidPcBrowser(), po.getMobileBrowserJsessionid(), po.getAppJsessionid(), vo.getId());
		return po;
	}

	@Override
	public Long findUserId(String loginName) {
		LoginUserMapping loginUserMapping = loginUserMappingDao.findByLoginName(loginName);
		return loginUserMapping==null?null:loginUserMapping.getUserId();
	}

	@Override
	@ReadOnly
	public User find(Long id, Client client) {
		UserDto dto = userDao.find(id);
		return dto==null?null:this.toUser(dto, client);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updatePassword(Long userId, String password) {
		userDao.updatePassword(password, userId);
	}

	protected User toUser(UserDto entity, Client client) {
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
			user.setJsessionid(entity.getPcBrowserJsessionid());
		else if(client.isMobile())
			user.setJsessionid(entity.getMobileBrowserJsessionid());
		else if(client.isApp())
			user.setJsessionid(entity.getAppJsessionid());
		return user;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Long add(UserDto vo) {
		this.userDao.save(vo.getId(), vo.getUsername(), vo.getName(), vo.getGender(), vo.getPassword(), vo.getMobilePhone(), vo.getEmail(), vo.getAvatar());
		return vo.getId();
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public boolean createMapping(String loginName, Long userId) {
		LoginUserMapping loginUserMapping = loginUserMappingDao.findByLoginName(loginName);
		if(loginUserMapping!=null) {
			return false;
		} else {
			this.doCreateMapping(loginName, userId);
			return true;
		}
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Long createMapping(String loginName) {
		LoginUserMapping loginUserMapping = loginUserMappingDao.findByLoginName(loginName);
		if(loginUserMapping!=null) {
			return null;
		} else {
			Long userId = utilsDao.selectAutoIncreament("b_user").longValue();
			this.doCreateMapping(loginName, userId);
			return userId;
		}
	}

	private void doCreateMapping(String loginName, Long userId) {
		loginUserMappingDao.save(loginName, userId);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Result updateMapping(String oldLoginName, String newLoginName, UserUpdation userUpdation) {
		LoginUserMapping loginUserMapping = loginUserMappingDao.findByLoginName(newLoginName);
		if(loginUserMapping!=null)//验重
			return new Result(0, "auth.mapping.new");
		loginUserMapping = loginUserMappingDao.findByLoginName(oldLoginName);
		Assert.notNull(loginUserMapping, "开发错误：旧手机号、邮箱、用户名不存在，请检查！");
		loginUserMappingDao.updateLoginName(newLoginName, loginUserMapping.getId());
		UserDto vo = new UserDto();
		vo.setId(loginUserMapping.getUserId());
		userUpdation.setLoginName(vo);
//		this.userRepository.save(vo);
		this.update(vo);
		return new Result(1, "status.success");
	}

	@Override
	public void deleteMappingAndUpdateUser(String oldLoginName, UserUpdation userUpdation, Long userId) {
		throw new RuntimeException("此方法专门提供给分片库模式，单库模式下禁调此方法！");
	}

	@Override
	public int updateJsessionidPcBrowser(Long id, String jsessionid) {
		return userDao.updateJsessionidPcBrowser(jsessionid, id);
	}

	@Override
	public int updateJsessionidMobileBrowser(Long id, String jsessionid) {
		return userDao.updateJsessionidMobileBrowser(jsessionid, id);
	}

	@Override
	public int updateJsessionidApp(Long id, String jsessionid) {
		return userDao.updateJsessionidApp(jsessionid, id);
	}
}