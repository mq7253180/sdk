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
		ModelAndView mv = new ModelAndView(InnerConstants.VIEW_PATH_RESULT);
		Result result = (Result)request.getSession().getAttribute("result");
		if(result!=null) {
			Object _status = request.getAttribute("status");
			if(_status==null)
				mv.addObject("status", result.getStatus());
			Object _msg = request.getAttribute("msg");
			if(_msg==null)
				mv.addObject("msg", result.getStatus());
		}
		return mv;
	}
}