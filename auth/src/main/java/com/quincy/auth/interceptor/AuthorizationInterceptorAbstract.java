package com.quincy.auth.interceptor;

import java.io.IOException;
import java.io.PrintWriter;
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
import com.quincy.sdk.Constants;
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
					request.setAttribute(Constants.ATTR_DENIED_PERMISSION, deniedPermissionName);
					this.output(request, response, handler, -1, requestContext.getMessage("status.error.403")+"["+deniedPermissionName+"]", "/auth/deny");
					return false;
				}
			}
			request.setAttribute(Constants.ATTR_SESSION, session);
			return true;
		}
	}

	private void output(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msg, String redirectTo) throws IOException, ServletException {
		String clientType = CommonHelper.clientType(request, handler);
		if(Constants.CLIENT_TYPE_J.equals(clientType)) {
			output(response, status, msg);
		} else {
			StringBuilder uri = new StringBuilder(200).append(redirectTo);
			String locale = CommonHelper.trim(request.getParameter(Constants.KEY_LOCALE));
			if(locale!=null)
				uri.append("?").append(Constants.KEY_LOCALE).append("=").append(locale);
			String requestURI = HttpClientHelper.getRequestURIOrURL(request, "URI");
			if(requestURI.indexOf("/index")>=0)
				requestURI = CommonHelper.trim(request.getParameter("back"));
			if(requestURI!=null)
				uri.append(uri.indexOf("?")>=0?"&":"?").append("back=").append(URLEncoder.encode(requestURI, "UTF-8"));
			request.getRequestDispatcher(uri.toString()).forward(request, response);
		}
	}

	public static void output(HttpServletResponse response, int status, String msg) throws IOException {
		String outputContent = "{\"status\":"+status+", \"msg\":\""+msg+"\"}";
		//ServletOutputStream out = null;
		PrintWriter out = null;
		try {
			//out = response.getOutputStream();
			response.setContentType("application/json;charset=utf-8");
			out = response.getWriter();
			out.println(outputContent);
			out.flush();
		} finally {
			if(out!=null)
				out.close();
		}
	}
}