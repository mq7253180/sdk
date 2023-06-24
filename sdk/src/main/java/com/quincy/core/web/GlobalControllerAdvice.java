package com.quincy.core.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalControllerAdvice {
	@Bean
	public GlobalHandlerExceptionResolver globalHandlerExceptionResolver() {
		return new GlobalHandlerExceptionResolver();
	}

	@Autowired
	private GlobalHandlerExceptionResolver handlerExceptionResolver;

    @ExceptionHandler
	public ModelAndView handleExeption(HttpServletRequest request, HttpServletResponse response, HandlerMethod hanlder, Exception e) {
		return handlerExceptionResolver.resolveException(request, response, hanlder, e);
	}
}