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

	@RequestMapping("/success")
	public ModelAndView success(HttpServletRequest request) {
		return this.createModelAndView(request, InnerConstants.VIEW_PATH_SUCCESS);
	}

	@RequestMapping("/failure")
	public ModelAndView error(HttpServletRequest request) {
		return this.createModelAndView(request, InnerConstants.VIEW_PATH_FAILURE);
	}

	private ModelAndView createModelAndView(HttpServletRequest request, String viewName) {
		ModelAndView mv = new ModelAndView(viewName);
		Result result = (Result)request.getSession().getAttribute("result");
		if(result!=null) {
			if(request.getAttribute("status")==null)
				mv.addObject("status", result.getStatus());
			if(request.getAttribute("msg")==null)
				mv.addObject("msg", result.getStatus());
			if(request.getAttribute("data")==null)
				mv.addObject("data", result.getData());
		}
		return mv;
	}
}