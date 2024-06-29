package com.quincy.core.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.core.AuthCommonConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeService;
import com.quincy.sdk.annotation.VCodeRequired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/vcode")
public class VCodeController extends HandlerInterceptorAdapter {
	@Autowired
	private VCodeService vCodeService;
	/**
	 * Example: 25/10/25/110/35
	 */
	@RequestMapping("/{size}/{start}/{space}/{width}/{height}")
	public void genVCode(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable(required = true, name = "size")int size,
			@PathVariable(required = true, name = "start")int start,
			@PathVariable(required = true, name = "space")int space,
			@PathVariable(required = true, name = "width")int width, 
			@PathVariable(required = true, name = "height")int height) throws Exception {
		vCodeService.outputVcode(request, response, size, start, space, width, height);
	}

	@RequestMapping("/failure")
	public ModelAndView vcodeFailure(HttpServletRequest request) {
		Result result = (Result)request.getSession().getAttribute("result");
		return new ModelAndView(InnerConstants.VIEW_PATH_RESULT)
				.addObject("status", result.getStatus())
				.addObject("msg", result.getMsg());
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			VCodeRequired annotation = method.getMethod().getDeclaredAnnotation(VCodeRequired.class);
			if(annotation!=null) {
				Result result = vCodeService.validateVCode(request, annotation.ignoreCase(), AuthCommonConstants.ATTR_KEY_VCODE_ROBOT_FORBIDDEN);
				if(result.getStatus()<1) {
					request.getSession().setAttribute("result", result);
					InnerHelper.outputOrForward(request, response, handler, result, "/vcode/failure", false);
					return false;
				}
			}
		}
		return true;
	}
}