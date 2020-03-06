package com.quincy.core.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "")
public class RootController {
	@RequestMapping(value = "/error")
	public ModelAndView handleError(HttpServletRequest request) {
		return new ModelAndView("/err");
	}
}