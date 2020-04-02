package com.quincy.core;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

public class InnerHelper {
	public static void outputOrRedirect(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msgI18NKey, String _redirectTo, boolean appendBackTo) throws IOException, ServletException {
		String clientType = CommonHelper.clientType(request, handler);
		if(InnerConstants.CLIENT_TYPE_J.equals(clientType)) {
			RequestContext requestContext = new RequestContext(request);
			String outputContent = "{\"status\":"+status+", \"msg\":\""+requestContext.getMessage(msgI18NKey)+"\"}";
			HttpClientHelper.outputJson(response, outputContent);
		} else {
			StringBuilder redirectTo = new StringBuilder(250).append(_redirectTo);
			if(appendBackTo)
				redirectTo
					.append(_redirectTo.indexOf("?")>=0?"&":"?")
					.append(InnerConstants.PARAM_REDIRECT_TO)
					.append("=")
					.append(URLEncoder.encode(request.getRequestURI()+(request.getQueryString()==null?"":("?"+request.getQueryString())), "UTF-8"));
			request.getRequestDispatcher(redirectTo.toString()).forward(request, response);
		}
	}
}