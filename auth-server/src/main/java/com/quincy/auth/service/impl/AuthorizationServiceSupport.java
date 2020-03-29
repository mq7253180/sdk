package com.quincy.auth.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.quincy.auth.entity.Menu;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.mapper.AuthMapper;
import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthorizationService;

public abstract class AuthorizationServiceSupport implements AuthorizationService {
	protected abstract Object getUserObject(HttpServletRequest request) throws Exception;

	public DSession getSession(HttpServletRequest request) throws Exception {
		Object obj = this.getUserObject(request);
		return obj==null?null:(DSession)obj;
	}

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