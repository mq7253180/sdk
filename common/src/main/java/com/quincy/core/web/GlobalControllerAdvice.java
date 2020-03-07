package com.quincy.core.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

//@ControllerAdvice
public class GlobalControllerAdvice {
//    @ExceptionHandler(value = Exception.class)
    public ModelAndView handleGlobleException(HttpServletRequest request, HttpServletResponse response, Exception e) {
    	return new GlobalHandlerExceptionResolver().resolveException(request, response, null, e);
    }
}