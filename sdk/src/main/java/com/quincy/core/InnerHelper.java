package com.quincy.core;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map.Entry;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
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
		outputOrRedirect(request, response, handler, result.getStatus(), result.getMsg(), result.getData(), redirectTo, appendBackTo);
	}

	public static void outputOrRedirect(HttpServletRequest request, HttpServletResponse response, Object handler, int status, String msg, Object data, String redirectTo, boolean appendBackTo) throws IOException, ServletException {
		Client client = Client.get(request, handler);
		boolean clientSys = redirectTo.startsWith("http");
		if(client.isJson()) {
			String outputContent = "{\"status\":"+status+", \"msg\":\""+msg+"\"";
			if(data!=null) {
				outputContent += ", \"data\": ";
				outputContent += new ObjectMapper().writeValueAsString(data);
			}
			outputContent += "}";
			HttpClientHelper.outputJson(response, outputContent);
		} else {
			StringBuilder location = new StringBuilder(280).append(redirectTo);
//			if(appendBackToFlag>APPEND_BACKTO_FLAG_NOT) {
			if(appendBackTo) {
				boolean appendRedirectTo = true;
				String requestURL = null;
				String queryString = CommonHelper.trim(request.getQueryString());
//				if(appendBackToFlag==APPEND_BACKTO_FLAG_URL) {
				if(clientSys) {
					requestURL = request.getRequestURL().toString();
					if(redirectTo.startsWith("https")&&requestURL.startsWith("http:"))
						requestURL = request.getRequestURL().insert(4, 's').toString();
					if(requestURL.endsWith("/"))
						requestURL = requestURL.substring(0, requestURL.length()-1);
//				} else if(appendBackToFlag==APPEND_BACKTO_FLAG_URI) {
				} else {
					requestURL = request.getRequestURI();
					if(queryString!=null||requestURL.length()>1) {
						if(requestURL.endsWith("/")&&requestURL.length()>1)
							requestURL = requestURL.substring(0, requestURL.length()-1);
					} else
						appendRedirectTo = false;
				}
				if(appendRedirectTo)
					location.append(getSeparater(location.toString()))
					.append(InnerConstants.PARAM_REDIRECT_TO)
					.append("=")
					.append(URLEncoder.encode(requestURL+(queryString==null?"":("?"+queryString)), "UTF-8"));
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
				session.setAttribute("data", data);
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

	public static ModelAndView modelAndViewMsg(HttpServletRequest request, int status, String msg, Object data, String successDestination) {
		HttpSession session = request.getSession(false);
		if(session!=null) {
			session.removeAttribute("status");
			session.removeAttribute("msg");
			session.removeAttribute("data");
		}
		String viewName = null;
		if(status==1) {
			viewName = successDestination==null?InnerConstants.VIEW_PATH_SUCCESS:successDestination.trim();
		} else
			viewName = InnerConstants.VIEW_PATH_FAILURE;
		return new ModelAndView(viewName)
				.addObject("status", status)
				.addObject("msg", msg)
				.addObject("data", data);
	}

	public static ModelAndView modelAndViewResult(HttpServletRequest request, Result result, String successDestination) {
		return modelAndViewMsg(request, result.getStatus(), result.getMsg(), result.getData(), successDestination);
	}

	public static ModelAndView modelAndViewResult(HttpServletRequest request, Result result) {
		return modelAndViewResult(request, result, null);
	}

	public static ModelAndView modelAndViewI18N(HttpServletRequest request, int status, String i18NKey, String successDestination) {
		return modelAndViewMsg(request, status, new RequestContext(request).getMessage(i18NKey), null, successDestination);
	}

	public static ModelAndView modelAndViewI18N(HttpServletRequest request, int status, String i18NKey) {
		return modelAndViewI18N(request, status, i18NKey, null);
	}
}