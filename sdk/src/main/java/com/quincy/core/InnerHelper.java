package com.quincy.core;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map.Entry;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class InnerHelper {
	public static void outputOrForward(HttpServletRequest request, HttpServletResponse response, Object handler, Result result, String redirectTo, boolean appendBackTo) throws IOException, ServletException {
		outputOrRedirect(request, response, handler, result.getStatus(), result.getMsg(), redirectTo, appendBackTo);
	}

	public static void outputOrRedirect(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msg, String redirectTo, boolean appendBackTo) throws IOException, ServletException {
		Client client = Client.get(request, handler);
		boolean clientSys = redirectTo.startsWith("http");
		if(client.isJson()) {
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
				if(appendRedirectTo)
					location.append(getSeparater(location.toString()))
					.append(InnerConstants.PARAM_REDIRECT_TO)
					.append("=")
					.append(URLEncoder.encode(requestURX+(queryString==null?"":("?"+queryString)), "UTF-8"));
			}
			if(clientSys) {
				String locale = CommonHelper.trim(request.getParameter(InnerConstants.KEY_LOCALE));
				if(locale!=null)
					location.append(getSeparater(location.toString()))
					.append(InnerConstants.KEY_LOCALE)
					.append("=")
					.append(locale);
				response.sendRedirect(location.toString());
			} else {
				HttpSession session = request.getSession();
				session.setAttribute("status", status);
				session.setAttribute("msg", msg);
				Iterator<Entry<String, String[]>> it = request.getParameterMap().entrySet().iterator();
				while(it.hasNext()) {
					Entry<String, String[]> e = it.next();
					if(e.getValue()!=null&&e.getValue().length>0&&!e.getKey().equals(InnerConstants.KEY_LOCALE)) {
						if(!"status".equals(e.getKey())&&!"msg".equals(e.getKey()))
							session.setAttribute(e.getKey(), e.getValue()[0]);
					}
				}
//				request.getRequestDispatcher(location.toString()).forward(request, response);
				response.sendRedirect(location.toString());
			}
		}
	}

	private static char getSeparater(String uri) {
		return uri.indexOf("?")>=0?'&':'?';
	}

	public static ModelAndView modelAndViewMsg(HttpServletRequest request, int status, String msg) {
		HttpSession session = request.getSession(false);
		if(session!=null) {
			session.removeAttribute("status");
			session.removeAttribute("msg");
			session.removeAttribute("data");
		}
		return new ModelAndView(status==1?InnerConstants.VIEW_PATH_SUCCESS:InnerConstants.VIEW_PATH_FAILURE)
				.addObject("status", status)
				.addObject("msg", msg);
	}

	public static ModelAndView modelAndViewI18N(HttpServletRequest request, int status, String i18NKey) {
		return modelAndViewMsg(request, status, new RequestContext(request).getMessage(i18NKey));
	}
}