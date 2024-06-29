package com.quincy.core.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalControllerAdvice {
	@Autowired
	private GlobalHandlerExceptionResolver handlerExceptionResolver;

    @ExceptionHandler
	public ModelAndView handleExeption(HttpServletRequest request, HttpServletResponse response, Exception e) {
		return handlerExceptionResolver.resolveException(request, response, null, e);
	}
}