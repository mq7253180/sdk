package com.quincy.core.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.HttpClientHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalHandlerExceptionResolver {//implements HandlerExceptionResolver {
//	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
		log.error(HttpClientHelper.getRequestURIOrURL(request, HttpClientHelper.FLAG_URL), e);
		Client client = Client.get(request, handler);
		String exception = null;
		if(!client.isJson()) {
			exception = getExceptionStackTrace(e, "<br/>", "&nbsp;");
		} else {
			exception = e.toString().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\\\", "/").replaceAll("\"", "'");
			response.setHeader("Content-Type", client.getContentType());
		}
//		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		RequestContext requestContext = new RequestContext(request);
		return new ModelAndView("/error_"+client.getSuffix())
				.addObject("msg", requestContext.getMessage(Result.I18N_KEY_EXCEPTION))
				.addObject("exception", exception);
	}

	public static String getExceptionStackTrace(Exception e, String lineBreak, String spaceSymbol) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		StringBuilder msg = new StringBuilder(500)
				.append("*************")
				.append(df.format(new Date()))
				.append("*************");
		appendCause(e, msg, lineBreak, spaceSymbol);
		return msg.toString();
	}

	private static void appendCause(Throwable e, StringBuilder msg, String lineBreak, String spaceSymbol) {
		if(e!=null) {
			msg.append(lineBreak).append(e.toString());
			StackTraceElement[] elements = e.getStackTrace();
			for(StackTraceElement element:elements) {
				msg.append(lineBreak);
				for(int j=0;j<10;j++)
					msg.append(spaceSymbol);
				msg.append("at").append(spaceSymbol).append(element.toString());
			}
			appendCause(e.getCause(), msg, lineBreak, spaceSymbol);
		}
	}
}