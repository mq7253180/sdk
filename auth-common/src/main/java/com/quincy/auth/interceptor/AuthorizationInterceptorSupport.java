package com.quincy.auth.interceptor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.AuthHelper;
import com.quincy.auth.o.XSession;
import com.quincy.core.AuthCommonConstants;
import com.quincy.core.InnerHelper;
import com.quincy.core.QuincyAuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AuthorizationInterceptorSupport extends HandlerInterceptorAdapter implements QuincyAuthInterceptor {
	@Autowired
	@Qualifier("signinUrl")
	private String signinUrl;
	@Autowired
	@Qualifier("denyUrl")
	private String denyUrl;

	protected boolean doAuth(HttpServletRequest request, HttpServletResponse response, Object handler, String permissionNeeded) throws Exception {
		XSession xsession = AuthHelper.getSession(request);//authorizationService.getSession(request);
		if(xsession==null) {
			InnerHelper.outputOrForward(request, response, handler, 0, new RequestContext(request).getMessage("auth.timeout.ajax"), signinUrl, true);
			return false;
		} else {
			if(permissionNeeded!=null) {
				List<String> permissions = xsession.getPermissions();
				boolean hasPermission = false;
				for(String permission:permissions) {
					if(permission.equals(permissionNeeded)) {
						hasPermission = true;
						break;
					}
				}
				if(!hasPermission) {
					String deniedPermissionName = AuthCommonConstants.PERMISSIONS==null?null:AuthCommonConstants.PERMISSIONS.get(permissionNeeded);
					if(deniedPermissionName==null)
						deniedPermissionName = permissionNeeded;
					request.setAttribute(AuthCommonConstants.ATTR_DENIED_PERMISSION, deniedPermissionName);
					InnerHelper.outputOrForward(request, response, handler, -1, new RequestContext(request).getMessage("status.error.403")+"["+deniedPermissionName+"]", denyUrl, true);
					return false;
				}
			}
//			request.setAttribute(InnerConstants.ATTR_SESSION, xsession);
			return true;
		}
	}
}