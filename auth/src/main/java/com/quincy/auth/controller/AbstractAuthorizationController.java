package com.quincy.auth.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.auth.service.AuthorizationService;
import com.quincy.sdk.Constants;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.CommonHelper;

@RequestMapping("/auth")
public abstract class AbstractAuthorizationController {
	@Autowired
	private AuthorizationService authorizationService;

	protected abstract User findUser(String username);
	protected abstract void updateLastLogin(Long userId, String jsessionid);
	/**
	 * 进登录页
	 */
	@RequestMapping("/signin")
	public ModelAndView toLogin(@RequestParam(required = false, value = "back")String _back) {
		ModelAndView mv = new ModelAndView("/login");
		String back = CommonHelper.trim(_back);
		if(back!=null)
			mv.addObject("back", back);
		return mv;
	}
	/**
	 * 进登录跳转页
	 */
	@RequestMapping("/signin/broker")
	public ModelAndView toLoginBroker(@RequestParam(required = false, value = "back")String _back) {
		ModelAndView mv = new ModelAndView("/login_broker");
		String back = CommonHelper.trim(_back);
		if(back!=null)
			mv.addObject("back", back);
		return mv;
	}
	/**
	 * 登出
	 */
	@RequestMapping("/signout")
	public ModelAndView logout(HttpServletRequest request) throws Exception {
		authorizationService.logout(request);
		RequestContext requestContext = new RequestContext(request);
		ModelAndView mv = new ModelAndView("/result");
		mv.addObject("status", 1);
		mv.addObject("msg", requestContext.getMessage("status.success"));
		return mv;
	}
	/**
	 * 点超链接没权限要进入的页面
	 */
	@RequestMapping("/deny")
	public String deny(HttpServletRequest request) throws Exception {
		return "/deny";
	}
	/**
	 * 登录
	 */
	@PostMapping("/signin/do")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = "username")String _username, 
			@RequestParam(required = false, value = "password")String _password, 
			@RequestParam(required = false, value = "back")String _back) throws Exception {
		Result result = this.login(request, _username, _password);
		String clientType = CommonHelper.clientType();
		ModelAndView mv = null;
		if(Constants.CLIENT_TYPE_J.equals(clientType)) {
			mv = this.createModelAndView(result);
		} else {
			if(result.getStatus()==1)
				mv = new ModelAndView("redirect:/index");
			else {
				mv = this.createModelAndView(result);
			}
		}
		return mv;
	}

	private ModelAndView createModelAndView(Result result) throws JsonProcessingException {
		ModelAndView mv = new ModelAndView("/result");
		mv.addObject("status", result.getStatus());
		mv.addObject("msg", result.getMsg());
		mv.addObject("data", new ObjectMapper().writeValueAsString(result.getData()));
		return mv;
	}

	private Result login(HttpServletRequest request, String _username, String _password) throws Exception {
		RequestContext requestContext = new RequestContext(request);
		Result result = new Result();
		String username = CommonHelper.trim(_username);
		if(username==null) {
			result.setStatus(-1);
			result.setMsg(requestContext.getMessage("auth.null.username"));
			return result;
		}
		String password = CommonHelper.trim(_password);
		if(password==null) {
			result.setStatus(-2);
			result.setMsg(requestContext.getMessage("auth.null.password"));
			return result;
		}
		User user = this.findUser(username);
		if(user==null) {
			result.setStatus(-3);
			result.setMsg(requestContext.getMessage("auth.account.no"));
			return result;
		}
		if(!password.equalsIgnoreCase(user.getPassword())) {
			result.setStatus(-4);
			result.setMsg(requestContext.getMessage("auth.account.pwd_incorrect"));
			return result;
		}
		DSession session = authorizationService.setSession(request, user.getJsessionid(), user.getId(), new AuthCallback() {
			@Override
			public void updateLastLogined(String jsessionid) {
				updateLastLogin(user.getId(), jsessionid);
			}

			@Override
			public User getUser() {
				return user;
			}
		});
		result.setStatus(1);
		result.setMsg(requestContext.getMessage("auth.success"));
		result.setData(session);
		return result;
	}
}
