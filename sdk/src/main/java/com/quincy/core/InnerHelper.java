package com.quincy.core;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

public class InnerHelper {
	public static void outputOrForward(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msg, String redirectTo, boolean appendBackTo) throws IOException, ServletException {
		String clientType = CommonHelper.clientType(request, handler);
		if(InnerConstants.CLIENT_TYPE_J.equals(clientType)) {
			String outputContent = "{\"status\":"+status+", \"msg\":\""+msg+"\"}";
			HttpClientHelper.outputJson(response, outputContent);
		} else {
			request.setAttribute("status", status);
			request.setAttribute("msg", msg);
			Iterator<Entry<String, String[]>> it = request.getParameterMap().entrySet().iterator();
			while(it.hasNext()) {
				Entry<String, String[]> e = it.next();
				if(e.getValue()!=null&&e.getValue().length>0&&!e.getKey().equals(InnerConstants.KEY_LOCALE))
					request.setAttribute(e.getKey(), e.getValue()[0]);
			}
			StringBuilder uri = new StringBuilder(280).append(redirectTo);
			if(appendBackTo) {
				String queryString = CommonHelper.trim(request.getQueryString());
				if(queryString!=null||request.getRequestURI().length()>1)
					uri.append(getSeparater(uri.toString()))
					.append(InnerConstants.PARAM_REDIRECT_TO)
					.append("=")
					.append(URLEncoder.encode(request.getRequestURI()+(queryString==null?"":("?"+queryString)), "UTF-8"));
			}
			request.getRequestDispatcher(uri.toString()).forward(request, response);
		}
	}

	private static char getSeparater(String uri) {
		return uri.indexOf("?")>=0?'&':'?';
	}
}