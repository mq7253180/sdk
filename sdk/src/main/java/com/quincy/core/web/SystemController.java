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
	public ModelAndView vcodeFailure(HttpServletRequest request) {
		ModelAndView mv = new ModelAndView(InnerConstants.VIEW_PATH_SUCCESS);
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