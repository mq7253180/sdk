package com.quincy.auth.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;

import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.mapper.AuthMapper;
import com.quincy.auth.service.XSessionService;
import com.quincy.sdk.annotation.jdbc.ReadOnly;
import com.quincy.sdk.o.Menu;
import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

public class XSessionServiceImpl implements XSessionService {
	@Autowired
	private AuthMapper authMapper;

	@ReadOnly
	@Override
	public XSession create(User user) {
		XSession session = new XSession();
		Long enterpriseId = user.getCurrentEnterprise()==null?null:user.getCurrentEnterprise().getId();
		//角色
		List<Role> roleList = authMapper.findRolesByUserIdAndEnterpriseId(user.getId(), enterpriseId);
		Map<Long, String> roleMap = new HashMap<Long, String>(roleList.size());
		for(Role role:roleList)//去重
			roleMap.put(role.getId(), role.getName());
		List<String> roles = new ArrayList<String>(roleMap.size());
		roles.addAll(roleMap.values());
		session.setRoles(roles);
		//权限
		List<Permission> permissionList = authMapper.findPermissionsByUserIdAndEnterpriseId(user.getId(), enterpriseId);
		Map<Long, String> permissionMap = new HashMap<Long, String>(permissionList.size());
		for(Permission permission:permissionList)//去重
			permissionMap.put(permission.getId(), permission.getName());
		List<String> permissions = new ArrayList<String>(permissionMap.size());
		permissions.addAll(permissionMap.values());
		session.setPermissions(permissions);
		//菜单
		List<Menu> rootMenus = this.findMenusByUserIdAndEnterpriseId(user.getId(), enterpriseId);
		session.setMenus(rootMenus);
		return session;
	}

	private List<Menu> findMenusByUserIdAndEnterpriseId(Long userId, Long enterpriseId) {
		List<Menu> allMenus = authMapper.findMenusByUserIdAndEnterpriseId(userId, enterpriseId);
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
}