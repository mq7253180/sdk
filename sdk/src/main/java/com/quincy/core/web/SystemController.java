package com.quincy.core.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.core.InnerConstants;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("")
public class SystemController {
	@GetMapping("/static/**")
	public void handleStatic() {}

	@RequestMapping("/success")
	public String success(HttpServletRequest request) {
		return InnerConstants.VIEW_PATH_SUCCESS;
	}

	@RequestMapping("/failure")
	public String failure(HttpServletRequest request) {
		return InnerConstants.VIEW_PATH_FAILURE;
	}
}