package com.quincy.sdk.view;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.Constants;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GlobalHandlerExceptionResolver implements HandlerExceptionResolver {
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
		log.error(HttpClientHelper.getRequestURIOrURL(request, "URL"), e);
		String clientType = CommonHelper.clientType(request, handler);
		String exception = Constants.CLIENT_TYPE_J.equals(clientType)?e.toString().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\\\", "/"):this.getExceptionStackTrace(e, "<br/>", "&nbsp;");
		RequestContext requestContext = new RequestContext(request);
		ModelAndView mv = new ModelAndView("/error_"+clientType);
		mv.addObject("msg", requestContext.getMessage(Result.I18N_KEY_EXCEPTION));
		mv.addObject("exception", exception);
		return mv;
	}

	private String getExceptionStackTrace(Exception e, String lineBreak, String spaceSymbol) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		StringBuilder msg = new StringBuilder(1000)
				.append("*************")
				.append(df.format(new Date()))
				.append("*************")
				.append(lineBreak).append(e.toString());
		StackTraceElement[] elements = e.getStackTrace();
		for(int i=0;i<elements.length;i++) {
			msg.append(lineBreak);
			for(int j=0;j<10;j++)
				msg.append(spaceSymbol);
			msg.append("at").append(spaceSymbol).append(elements[i].toString());
		}
		return msg.toString();
	}
}