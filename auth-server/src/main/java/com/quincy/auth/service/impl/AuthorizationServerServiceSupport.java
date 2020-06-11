package com.quincy.auth.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.quincy.auth.entity.MenuEntity;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.mapper.AuthMapper;
import com.quincy.auth.o.DSession;
import com.quincy.auth.o.Menu;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthorizationServerService;

public abstract class AuthorizationServerServiceSupport implements AuthorizationServerService {
	@Autowired
	private AuthMapper authMapper;
	@Value("${auth.enterprise}")
	private boolean isEnterprise;

	protected DSession createSession(Long userId) {
		DSession session = new DSession();
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

	protected DSession createSession(User user) {
		DSession session = this.createSession(user.getId());
		session.setUser(user);
		return session;
	}

	private List<Menu> findMenusByUserId(Long userId) {
		List<MenuEntity> allMenus = authMapper.findMenusByUserId(userId);
		Map<Long, MenuEntity> duplicateRemovedMenus = new HashMap<Long, MenuEntity>(allMenus.size());
		for(MenuEntity menu:allMenus)
			duplicateRemovedMenus.put(menu.getId(), menu);
		List<Menu> rootMenus = new ArrayList<Menu>(duplicateRemovedMenus.size());
		Set<Entry<Long, MenuEntity>> entrySet = duplicateRemovedMenus.entrySet();
		for(Entry<Long, MenuEntity> entry:entrySet) {
			MenuEntity menu = entry.getValue();
			if(menu.getPId()==null) {
				rootMenus.add(menu);
				this.loadChildrenMenus(menu, entrySet);
			}
		}
		return rootMenus;
	}

	private void loadChildrenMenus(MenuEntity parent, Set<Entry<Long, MenuEntity>> entrySet) {
		for(Entry<Long, MenuEntity> entry:entrySet) {
			MenuEntity menu = entry.getValue();
			if(parent.getId()==menu.getPId()) {
				if(parent.getChildren()==null)
					parent.setChildren(new ArrayList<MenuEntity>(10));
				parent.getChildren().add(menu);
			}
		}
		if(parent.getChildren()!=null&&parent.getChildren().size()>0) {
			for(MenuEntity child:parent.getChildren())
				this.loadChildrenMenus(child, entrySet);
		}
	}
}