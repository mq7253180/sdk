package com.quincy.auth.interceptor;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.AuthConstants;
import com.quincy.core.InnerHelper;
import com.quincy.core.web.QuincyAuthInterceptor;
import com.quincy.sdk.AuthHelper;
import com.quincy.sdk.o.XSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AuthorizationInterceptorSupport extends HandlerInterceptorAdapter implements QuincyAuthInterceptor {
	@Value("${auth.center:}")
	private String authCenter;

	protected boolean doAuth(HttpServletRequest request, HttpServletResponse response, Object handler, String permissionNeeded) throws Exception {
		XSession xsession = AuthHelper.getSession(request);//authorizationService.getSession(request);
		if(xsession==null) {
			InnerHelper.outputOrRedirect(request, response, handler, 0, new RequestContext(request).getMessage("auth.timeout.ajax"), null, authCenter+"/auth/signin/broker", true);
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
					String deniedPermissionName = AuthConstants.PERMISSIONS==null?null:AuthConstants.PERMISSIONS.get(permissionNeeded);
					if(deniedPermissionName==null)
						deniedPermissionName = permissionNeeded;
					request.setAttribute(AuthConstants.ATTR_DENIED_PERMISSION, deniedPermissionName);
					InnerHelper.outputOrRedirect(request, response, handler, -1, new RequestContext(request).getMessage("status.error.403")+"["+deniedPermissionName+"]", null, authCenter+"/auth/deny", true);
					return false;
				}
			}
//			request.setAttribute(InnerConstants.ATTR_SESSION, xsession);
			return true;
		}
	}
}