package com.quincy.auth.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.OAuth2ResourceConstants;

@Controller
@RequestMapping("/oauth2")
public class OAuth2ResourceController {
	@RequestMapping("/error/resource")
	public ModelAndView error(HttpServletRequest request, @RequestParam(required = true, value = "status")int status) {
		return new ModelAndView("/oauth2_error").addObject("msg", new RequestContext(request).getMessage(OAuth2ResourceConstants.RESOURCE_ERROR_MSG_KEY_PREFIX+status));
	}
}