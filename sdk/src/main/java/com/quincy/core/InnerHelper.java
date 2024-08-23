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

	public static ModelAndView modelAndViewMsg(HttpServletRequest request, int status, String msg, Object data, String destination, boolean redirect) {
		HttpSession session = request.getSession(false);
		if(session!=null) {
			session.removeAttribute("status");
			session.removeAttribute("msg");
			session.removeAttribute("data");
		}
		String viewName = null;
		if(status==1) {
			if(destination!=null&&!Client.get(request).isJson()) {
				if(redirect) {
					viewName = "redirect:"+destination;
				} else
					viewName = destination;
			}
			if(viewName==null)
				viewName = InnerConstants.VIEW_PATH_SUCCESS;
		} else
			viewName = InnerConstants.VIEW_PATH_FAILURE;
		return new ModelAndView(viewName)
				.addObject("status", status)
				.addObject("msg", msg)
				.addObject("data", data);
	}

	public static ModelAndView modelAndViewMsg(HttpServletRequest request, Result result, String destination, boolean redirect) {
		return modelAndViewMsg(request, result.getStatus(), result.getMsg(), result.getData(), destination, redirect);
	}

	public static ModelAndView modelAndViewMsg(HttpServletRequest request, Result result) {
		return modelAndViewMsg(request, result, null, false);
	}

	public static ModelAndView modelAndViewI18N(HttpServletRequest request, int status, String i18NKey, String destination, boolean redirect) {
		return modelAndViewMsg(request, status, new RequestContext(request).getMessage(i18NKey), null, destination, redirect);
	}

	public static ModelAndView modelAndViewI18N(HttpServletRequest request, int status, String i18NKey) {
		return modelAndViewI18N(request, status, i18NKey, null, false);
	}
}