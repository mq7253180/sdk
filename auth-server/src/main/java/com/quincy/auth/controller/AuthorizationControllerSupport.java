package com.quincy.auth.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.AuthConstants;
import com.quincy.auth.o.DSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.auth.service.AuthorizationService;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.CommonHelper;

@RequestMapping("/auth")
public abstract class AuthorizationControllerSupport {
	@Autowired
	private AuthorizationService authorizationService;

	protected abstract User findUser(String username, Client client);
	protected abstract void updateLastLogin(Long userId, Client client, String jsessionid);
	protected abstract ModelAndView signinView(HttpServletRequest request);
	/**
	 * 进登录页
	 */
	@RequestMapping("/signin")
	public ModelAndView toLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo) {
		ModelAndView mv = signinView(request);
		if(mv==null)
			mv = new ModelAndView("/login");
		String redirectTo = CommonHelper.trim(_redirectTo);
		if(redirectTo!=null)
			mv.addObject(InnerConstants.PARAM_REDIRECT_TO, redirectTo);
		return mv;
	}
	/**
	 * 进登录跳转页
	 */
	@RequestMapping("/signin/broker")
	public ModelAndView toLoginBroker(@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo) {
		ModelAndView mv = new ModelAndView("/login_broker");
		String redirectTo = CommonHelper.trim(_redirectTo);
		if(redirectTo!=null)
			mv.addObject(InnerConstants.PARAM_REDIRECT_TO, redirectTo);
		return mv;
	}
	/**
	 * 登出
	 */
	@RequestMapping("/signout")
	public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
		authorizationService.logout(request, response);
		RequestContext requestContext = new RequestContext(request);
		return new ModelAndView("/result").addObject("status", 1).addObject("msg", requestContext.getMessage("status.success"));
	}
	/**
	 * 点超链接没权限要进入的页面
	 */
	@RequestMapping("/deny")
	public String deny(HttpServletRequest request) throws Exception {
		return "/deny";
	}

	protected Result doPwdLogin(HttpServletRequest request, String username, String _password) throws Exception {
		String password = CommonHelper.trim(_password);
		Result result = password!=null?login(request, username, password):new Result(0, new RequestContext(request).getMessage("auth.null.password"));
		return result;
	}

	protected Result login(HttpServletRequest request, String _username, String password) throws Exception {
		RequestContext requestContext = new RequestContext(request);
		Result result = new Result();
		String username = CommonHelper.trim(_username);
		if(username==null) {
			result.setStatus(-1);
			result.setMsg(requestContext.getMessage("auth.null.username"));
			return result;
		}
		Client client = CommonHelper.getClient(request);
		User user = findUser(username, client);
		if(user==null) {
			result.setStatus(-2);
			result.setMsg(requestContext.getMessage("auth.account.no"));
			return result;
		}
		if(password!=null&&!password.equalsIgnoreCase(user.getPassword())) {
			result.setStatus(AuthConstants.LOGIN_STATUS_PWD_INCORRECT);
			result.setMsg(requestContext.getMessage("auth.account.pwd_incorrect"));
			return result;
		}
		DSession session = authorizationService.setSession(request, CommonHelper.trim(user.getJsessionid()), user.getId(), new AuthCallback() {
			@Override
			public void updateLastLogined(String jsessionid) {
				updateLastLogin(user.getId(), client, jsessionid);
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

	protected ModelAndView createModelAndView(HttpServletRequest request, Result result, String _redirectTo) throws JsonProcessingException {
		String clientType = CommonHelper.clientType(request);
		ModelAndView mv = null;
		if(InnerConstants.CLIENT_TYPE_J.equals(clientType)) {
			mv = createModelAndView(result);
		} else {
			if(result.getStatus()==1) {
				String redirectTo = CommonHelper.trim(_redirectTo);
				mv = new ModelAndView("redirect:"+(redirectTo!=null?redirectTo:AuthConstants.URI_INDEX));
			} else
				mv = createModelAndView(result);
		}
		return mv;
	}

	private ModelAndView createModelAndView(Result result) throws JsonProcessingException {
		return new ModelAndView("/result")
				.addObject("status", result.getStatus())
				.addObject("msg", result.getMsg())
				.addObject("data", new ObjectMapper().writeValueAsString(result.getData()));
	}
}