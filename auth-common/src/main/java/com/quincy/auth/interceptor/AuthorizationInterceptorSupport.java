package com.quincy.auth.interceptor;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.AuthCommonConstants;
import com.quincy.auth.o.DSession;
import com.quincy.auth.service.AuthorizationCommonService;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;

public abstract class AuthorizationInterceptorSupport extends HandlerInterceptorAdapter {
	@Autowired
	private AuthorizationCommonService authorizationService;
	@Resource(name = "signinUrl")
	private String signinUrl;
	@Resource(name = "denyUrl")
	private String denyUrl;

	protected boolean doAuth(HttpServletRequest request, HttpServletResponse response, Object handler, String permissionNeeded) throws Exception {
		DSession session = authorizationService.getSession(request);
		if(session==null) {
			InnerHelper.outputOrForward(request, response, handler, 0, new RequestContext(request).getMessage("auth.timeout.ajax"), signinUrl, true);
			return false;
		} else {
			if(permissionNeeded!=null) {
				List<String> permissions = session.getPermissions();
				boolean hasPermission = false;
				for(String permission:permissions) {
					if(permission.equals(permissionNeeded)) {
						hasPermission = true;
						break;
					}
				}
				if(!hasPermission) {
					String deniedPermissionName = AuthCommonConstants.PERMISSIONS.get(permissionNeeded);
					if(deniedPermissionName==null)
						deniedPermissionName = permissionNeeded;
					request.setAttribute(AuthCommonConstants.ATTR_DENIED_PERMISSION, deniedPermissionName);
					InnerHelper.outputOrForward(request, response, handler, -1, new RequestContext(request).getMessage("status.error.403")+"["+deniedPermissionName+"]", denyUrl, true);
					return false;
				}
			}
			request.setAttribute(InnerConstants.ATTR_SESSION, session);
			return true;
		}
	}

	protected void setExpiry(HttpServletRequest request) throws Exception {
		authorizationService.setExpiry(request);
	}
}