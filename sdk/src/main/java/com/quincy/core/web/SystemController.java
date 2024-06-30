package com.quincy.core.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.Result;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("")
public class SystemController {
	@GetMapping("/static/**")
	public void handleStatic() {}

	@RequestMapping("/result")
	public ModelAndView vcodeFailure(HttpServletRequest request) {
		Result result = (Result)request.getSession().getAttribute("result");
		return new ModelAndView(InnerConstants.VIEW_PATH_RESULT)
				.addObject("status", result.getStatus())
				.addObject("msg", result.getMsg());
	}
}