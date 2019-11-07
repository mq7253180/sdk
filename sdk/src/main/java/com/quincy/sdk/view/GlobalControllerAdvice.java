package com.quincy.sdk.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.Result;
import com.quincy.sdk.helper.HttpClientHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@ControllerAdvice
public class GlobalControllerAdvice {
    @ResponseBody
//    @ExceptionHandler(value = Exception.class)
    public Result handleGlobleException(HttpServletRequest request, Exception e) throws IOException {
    		log.error("EXCEPTION_MSG: "+HttpClientHelper.getRequestURIOrURL(request, "URL"), e);
    		RequestContext requestContext = new RequestContext(request);
    		Result result = Result.newException();
    		result.msg(requestContext.getMessage(Result.I18N_KEY_EXCEPTION)).data(e.getClass().getName()+": "+e.getMessage());
    		return result;
    }
}
