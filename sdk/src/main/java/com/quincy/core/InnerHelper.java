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
	public final static int APPEND_BACKTO_FLAG_NOT = 0;
	public final static int APPEND_BACKTO_FLAG_URI = 1;
	public final static int APPEND_BACKTO_FLAG_URL = 2;

	public static void outputOrForward(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msg, String redirectTo, boolean appendBackTo) throws IOException, ServletException {
		String clientType = CommonHelper.clientType(request, handler);
		boolean clientSys = redirectTo.startsWith("http");
		if(InnerConstants.CLIENT_TYPE_J.equals(clientType)) {
			String outputContent = "{\"status\":"+status+", \"msg\":\""+msg+"\"}";
			HttpClientHelper.outputJson(response, outputContent);
		} else {
			StringBuilder location = new StringBuilder(280).append(redirectTo);
//			if(appendBackToFlag>APPEND_BACKTO_FLAG_NOT) {
			if(appendBackTo) {
				boolean appendRedirectTo = true;
				String requestURX = null;
				String queryString = CommonHelper.trim(request.getQueryString());
//				if(appendBackToFlag==APPEND_BACKTO_FLAG_URL) {
				if(clientSys) {
					requestURX = request.getRequestURL().toString();
					if(requestURX.endsWith("/"))
						requestURX = requestURX.substring(0, requestURX.length()-1);
//				} else if(appendBackToFlag==APPEND_BACKTO_FLAG_URI) {
				} else {
					requestURX = request.getRequestURI();
					if(queryString!=null||requestURX.length()>1) {
						if(requestURX.endsWith("/")&&requestURX.length()>1)
							requestURX = requestURX.substring(0, requestURX.length()-1);
					} else
						appendRedirectTo = false;
				}
				String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
				if(locale!=null)
					location.append(getSeparater(location.toString()))
					.append(InnerConstants.KEY_LOCALE)
					.append("=")
					.append(locale);
				if(appendRedirectTo)
					location.append(getSeparater(location.toString()))
					.append(InnerConstants.PARAM_REDIRECT_TO)
					.append("=")
					.append(URLEncoder.encode(requestURX+(queryString==null?"":("?"+queryString)), "UTF-8"));
			}
			if(clientSys) {
				response.sendRedirect(location.toString());
			} else {
				request.setAttribute("status", status);
				request.setAttribute("msg", msg);
				Iterator<Entry<String, String[]>> it = request.getParameterMap().entrySet().iterator();
				while(it.hasNext()) {
					Entry<String, String[]> e = it.next();
					if(e.getValue()!=null&&e.getValue().length>0&&!e.getKey().equals(InnerConstants.KEY_LOCALE))
						request.setAttribute(e.getKey(), e.getValue()[0]);
				}
				request.getRequestDispatcher(location.toString()).forward(request, response);
			}
		}
	}

	private static char getSeparater(String uri) {
		return uri.indexOf("?")>=0?'&':'?';
	}
}