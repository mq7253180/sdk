package com.quincy.core.web;

import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//@ControllerAdvice
public class GlobalControllerAdvice {
//    @ExceptionHandler(value = Exception.class)
    public ModelAndView handleGlobleException(HttpServletRequest request, HttpServletResponse response, Exception e) {
    	return new GlobalHandlerExceptionResolver().resolveException(request, response, null, e);
    }
}