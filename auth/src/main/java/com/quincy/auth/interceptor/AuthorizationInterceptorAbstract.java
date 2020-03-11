package com.quincy.auth.interceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.AuthConstants;
import com.quincy.auth.o.DSession;
import com.quincy.auth.service.AuthorizationService;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

public abstract class AuthorizationInterceptorAbstract extends HandlerInterceptorAdapter {
	@Autowired
	private AuthorizationService authorizationService;

	protected boolean doAuth(HttpServletRequest request, HttpServletResponse response, Object handler, String permissionNeeded) throws Exception {
		DSession session = authorizationService.getSession(request);
		RequestContext requestContext = new RequestContext(request);
		if(session==null) {
			this.output(request, response, handler, 0, requestContext.getMessage("auth.timeout.ajax"), "/auth/signin/broker");
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
					String deniedPermissionName = AuthConstants.PERMISSIONS.get(permissionNeeded);
					if(deniedPermissionName==null)
						deniedPermissionName = permissionNeeded;
//					authorizationService.setDeniedPermissionName(request, deniedPermissionName);
					request.setAttribute(InnerConstants.ATTR_DENIED_PERMISSION, deniedPermissionName);
					this.output(request, response, handler, -1, requestContext.getMessage("status.error.403")+"["+deniedPermissionName+"]", "/auth/deny");
					return false;
				}
			}
			request.setAttribute(InnerConstants.ATTR_SESSION, session);
			return true;
		}
	}

	private void output(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msg, String redirectTo) throws IOException, ServletException {
		String clientType = CommonHelper.clientType(request, handler);
		if(InnerConstants.CLIENT_TYPE_J.equals(clientType)) {
			String outputContent = "{\"status\":"+status+", \"msg\":\""+msg+"\"}";
			HttpClientHelper.outputJson(response, outputContent);
		} else
			request.getRequestDispatcher(new StringBuilder(250)
					.append(redirectTo)
					.append(redirectTo.indexOf("?")>=0?"&":"?")
					.append(AuthConstants.PARAM_BACK_TO)
					.append("=")
					.append(URLEncoder.encode(request.getRequestURI()+"?"+request.getQueryString(), "UTF-8"))
					.toString()).forward(request, response);
	}
}