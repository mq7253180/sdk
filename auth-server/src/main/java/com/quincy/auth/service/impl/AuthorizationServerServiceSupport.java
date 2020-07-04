package com.quincy.auth.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.mapper.AuthMapper;
import com.quincy.auth.o.XSession;
import com.quincy.auth.o.Menu;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthorizationServerService;

public abstract class AuthorizationServerServiceSupport implements AuthorizationServerService {
	@Autowired
	private AuthMapper authMapper;
	@Value("${auth.enterprise}")
	private boolean isEnterprise;

	protected XSession createSession(Long userId) {
		XSession session = new XSession();
		if(isEnterprise) {
			//角色
			List<Role> roleList = authMapper.findRolesByUserId(userId);
			Map<Long, String> roleMap = new HashMap<Long, String>(roleList.size());
			for(Role role:roleList)//去重
				roleMap.put(role.getId(), role.getName());
			List<String> roles = new ArrayList<String>(roleMap.size());
			roles.addAll(roleMap.values());
			session.setRoles(roles);
			//权限
			List<Permission> permissionList = authMapper.findPermissionsByUserId(userId);
			Map<Long, String> permissionMap = new HashMap<Long, String>(permissionList.size());
			for(Permission permission:permissionList)//去重
				permissionMap.put(permission.getId(), permission.getName());
			List<String> permissions = new ArrayList<String>(permissionMap.size());
			permissions.addAll(permissionMap.values());
			session.setPermissions(permissions);
			//菜单
			List<Menu> rootMenus = this.findMenusByUserId(userId);
			session.setMenus(rootMenus);
		}
		return session;
	}

	protected XSession createSession(User user) {
		XSession session = this.createSession(user.getId());
		session.setUser(user);
		return session;
	}

	private List<Menu> findMenusByUserId(Long userId) {
		List<Menu> allMenus = authMapper.findMenusByUserId(userId);
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
}