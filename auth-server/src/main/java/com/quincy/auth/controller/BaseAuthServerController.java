package com.quincy.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.AuthConstants;
import com.quincy.auth.SessionInvalidation;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.XSessionService;
import com.quincy.core.VCodeConstants;
import com.quincy.sdk.AuthActions;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class BaseAuthServerController {
	@Autowired(required = false)
	private UserService userService;
	@Autowired(required = false)
	private XSessionService xSessionService;
	@Autowired(required = false)
	private SessionInvalidation sessionInvalidation;
	@Autowired(required = false)
	private AuthActions authActions;
	@Value("${server.servlet.session.timeout.mobile:#{null}}")
	private String mobileSessionTimeout;
	@Value("${server.servlet.session.timeout.app:#{null}}")
	private String appSessionTimeout;
	protected final static int LOGIN_STATUS_PWD_INCORRECT = -3;
	protected final static String AUTH_ACTIONS_NULL_MSG = "没有设置回调动作";

	protected Result login(HttpServletRequest request, String _username) throws Exception {
		return this.login(request, _username, null);
	}

	protected Result login(HttpServletRequest request, String _username, String password) throws Exception {
		Assert.notNull(authActions, AUTH_ACTIONS_NULL_MSG);
		RequestContext requestContext = new RequestContext(request);
		Result result = new Result();
		String username = CommonHelper.trim(_username);
		if(username==null) {
			result.setStatus(-1);
			result.setMsg(requestContext.getMessage("auth.null.username"));
			return result;
		}
		Client client = Client.get(request);
		User user = userService.find(username, client);
		if(user==null) {
			result.setStatus(-2);
			result.setMsg(requestContext.getMessage("auth.account.no"));
			return result;
		}
		if(password!=null&&!password.equalsIgnoreCase(user.getPassword())) {
			result.setStatus(LOGIN_STATUS_PWD_INCORRECT);
			result.setMsg(requestContext.getMessage("auth.account.pwd_incorrect"));
			return result;
		}
		HttpSession session = request.getSession();//所有身份认证通过，创建session
		if(appSessionTimeout!=null&&client.isApp()) {//APP设置超时时间
			session.setMaxInactiveInterval(Integer.parseInt(String.valueOf(Duration.parse(appSessionTimeout).getSeconds())));
		} else if(mobileSessionTimeout!=null&&client.isMobile()) {//移动设置网页设置超时时间
			session.setMaxInactiveInterval(Integer.parseInt(String.valueOf(Duration.parse(mobileSessionTimeout).getSeconds())));
		} else {//网页端，验证码登录临时保存验证码时会给session设置一个较短的超时时间，登录成功后在这里给恢复回来
			Object maxInactiveInterval = session.getAttribute(VCodeConstants.ATTR_KEY_VCODE_ORIGINAL_MAX_INACTIVE_INTERVAL);
			if(maxInactiveInterval!=null)
				session.setMaxInactiveInterval(Integer.parseInt(maxInactiveInterval.toString()));
		}
		String originalJsessionid = user.getJsessionid();
		user.setJsessionid(session.getId());
		XSession xsession = xSessionService==null?new XSession():xSessionService.create(user);
		xsession.setUser(user);
		session.setAttribute(AuthConstants.ATTR_SESSION, xsession);
		if(sessionInvalidation!=null) {//同一user同一类端之间互踢，清除session
			originalJsessionid = CommonHelper.trim(originalJsessionid);
			if(originalJsessionid!=null&&!originalJsessionid.equals(session.getId())) {
				if((client.isPc()&&sessionInvalidation.pcBrowserEvict())//PC浏览器互踢
						||(client.isMobile()&&sessionInvalidation.mobileBrowserEvict())//移动设备浏览器互踢
						||(client.isApp()&&sessionInvalidation.appEvict())//APP互踢
					)
					sessionInvalidation.invalidate(originalJsessionid);
			}
		}
		this.updateLastLogin(user.getId(), session.getId(), client);//互踢还需要应用层配合更新数据库里的jsessionid
		result.setStatus(1);
		result.setMsg(requestContext.getMessage("auth.success"));
		result.setData(client.isJson()?new ObjectMapper().writeValueAsString(xsession):xsession);
		return result;
	}

	private void updateLastLogin(Long userId, String jessionid, Client client) {
		UserEntity vo = new UserEntity();
		vo.setId(userId);
		if(client.isPc())
			vo.setJsessionidPcBrowser(jessionid);
		if(client.isMobile())
			vo.setJsessionidMobileBrowser(jessionid);
		if(client.isApp())
			vo.setJsessionidApp(jessionid);
		userService.update(vo);
	}
}