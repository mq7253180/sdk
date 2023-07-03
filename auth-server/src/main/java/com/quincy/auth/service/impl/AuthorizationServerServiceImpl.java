package com.quincy.auth.service.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quincy.auth.AuthSessionHolder;
import com.quincy.auth.entity.Permission;
import com.quincy.auth.entity.Role;
import com.quincy.auth.mapper.AuthMapper;
import com.quincy.auth.o.XSession;
import com.quincy.auth.o.Menu;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.auth.service.AuthorizationServerService;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthorizationServerServiceImpl implements AuthorizationServerService {
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

	private void excludeSession(String originalJsessionid) {
		HttpSession httpSession = AuthSessionHolder.SESSIONS.remove(originalJsessionid);
		if(httpSession!=null)
			httpSession.invalidate();
	}

	@Value("${server.servlet.session.timeout:#{null}}")
	private String sessionTimeout;
	@Value("${server.servlet.session.timeout.app:#{null}}")
	private String sessionTimeoutApp;

	@Override
	public XSession setSession(HttpServletRequest request, AuthCallback callback) {
		User user = callback.getUser();
		String originalJsessionid = CommonHelper.trim(user.getJsessionid());
		if(originalJsessionid!=null)//同一user不同客户端登录互踢
			this.excludeSession(originalJsessionid);
		int maxInactiveInterval = -1;
		if(CommonHelper.isApp(request)) {
			maxInactiveInterval = sessionTimeoutApp==null?86400:Integer.parseInt(String.valueOf(Duration.parse(sessionTimeoutApp).getSeconds()));
		} else
			maxInactiveInterval = sessionTimeout==null?18000:Integer.parseInt(String.valueOf(Duration.parse(sessionTimeout).getSeconds()));
		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(maxInactiveInterval);//验证码接口会设置一个较短的超时时间，登录成功后在这里给恢复回来，如果没有设置取默认半小时
		String jsessionid = session.getId();
		user.setJsessionid(jsessionid);
		XSession xsession = this.createSession(user);
		session.setAttribute(InnerConstants.ATTR_SESSION, xsession);
		callback.updateLastLogined(jsessionid);
		return xsession;
	}

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
}