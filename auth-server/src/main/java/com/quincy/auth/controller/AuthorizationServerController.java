package com.quincy.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.AuthConstants;
import com.quincy.auth.TempPwdLoginEmailInfo;
import com.quincy.auth.o.XSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.XSessionService;
import com.quincy.core.VCodeConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.core.SessionInvalidation;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeOpsRgistry;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthorizationServerController {
	@Autowired(required = false)
	private XSessionService xSessionService;
	@Autowired(required = false)
	private AuthActions authActions;
	@Autowired(required = false)
	private SessionInvalidation sessionInvalidation;
	@Autowired
	private VCodeOpsRgistry vCodeOpsRgistry;
	@Autowired(required = false)
	private TempPwdLoginEmailInfo tempPwdLoginEmailInfo;
	@Value("${server.servlet.session.timeout.mobile:#{null}}")
	private String mobileSessionTimeout;
	@Value("${server.servlet.session.timeout.app:#{null}}")
	private String appSessionTimeout;
	private final static int LOGIN_STATUS_PWD_INCORRECT = -3;
	private final static String PARA_NAME_USERNAME = "username";
	private final static String AUTH_ACTIONS_NULL_MSG = "没有设置回调动作";
	/**
	 * 进登录页
	 */
	@RequestMapping("/signin")
	public ModelAndView toLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo) {
		Assert.notNull(authActions, AUTH_ACTIONS_NULL_MSG);
		ModelAndView mv = authActions.signinView(request);
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
	public ModelAndView toLoginBroker(@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo, @RequestParam(required = false, value = InnerConstants.KEY_LOCALE)String _locale) {
		ModelAndView mv = new ModelAndView("/login_broker");
		String redirectTo = CommonHelper.trim(_redirectTo);
		if(redirectTo!=null)
			mv.addObject(InnerConstants.PARAM_REDIRECT_TO, redirectTo);
		String locale = CommonHelper.trim(_locale);
		mv.addObject(InnerConstants.KEY_LOCALE, locale==null?"":locale);
		return mv;
	}

	private Result login(HttpServletRequest request, String _username, String password) throws Exception {
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
		User user = authActions.findUser(username, client);
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
			if(originalJsessionid!=null) {
				if((client.isPc()&&sessionInvalidation.pcBrowserEvict())//PC浏览器互踢
						||(client.isMobile()&&sessionInvalidation.mobileBrowserEvict())//移动设备浏览器互踢
						||(client.isApp()&&sessionInvalidation.appEvict())//APP互踢
					)
					sessionInvalidation.invalidate(originalJsessionid);
			}
		}
		authActions.updateLastLogin(user, session.getId(), client);//互踢还需要应用层配合更新数据库里的jsessionid
		result.setStatus(1);
		result.setMsg(requestContext.getMessage("auth.success"));
		result.setData(client.isJson()?new ObjectMapper().writeValueAsString(xsession):xsession);
		return result;
	}

	protected ModelAndView createModelAndView(HttpServletRequest request, Result result, String _redirectTo) throws JsonProcessingException {
		Client client = Client.get(request);
		ModelAndView mv = null;
		if(client.isJson()) {
			mv = createModelAndView(request.getSession(), result);
		} else {
			if(result.getStatus()==1) {
				String redirectTo = CommonHelper.trim(_redirectTo);
				mv = new ModelAndView("redirect:"+(redirectTo!=null?redirectTo:""));
			} else
				mv = createModelAndView(request.getSession(), result);
		}
		return mv;
	}

	private ModelAndView createModelAndView(HttpSession session, Result result) throws JsonProcessingException {
		session.setAttribute("status", result.getStatus());
		session.setAttribute("msg", result.getMsg());
		session.setAttribute("data", result.getData());
		return new ModelAndView("/result_login")
				.addObject("status", result.getStatus())
				.addObject("msg", result.getMsg())
				.addObject("data", result.getData());
	}

	private int getFailures(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if(session==null)
			return 0;
		Object _failures = session.getAttribute(LOGIN_FAILURES_HOLDER_KEY);
		return _failures==null?0:Integer.parseInt(_failures.toString());
	}

	@Value("${auth.vcode.loginFailures}")
	private int failuresThresholdForVCode;
	private final static String LOGIN_FAILURES_HOLDER_KEY = "login_failures";
	/**
	 * 密码登录
	 */
	@PostMapping("/signin/pwd")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = "password")String password, 
			@RequestParam(required = false, value = VCodeConstants.PARA_NAME_VCODE)String vcode, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo
			) throws Exception {
		Result result = null;
		if(failuresThresholdForVCode>=Integer.MAX_VALUE) {//设置为最大值时不验证失败次数
			result = pwdLogin(request, username, password);
		} else {
			int failures = this.getFailures(request);
			if(failures<failuresThresholdForVCode) {//小于失败次数
				result = pwdLogin(request, username, password);
			} else {//失败次数满，需要验证码
				result = vCodeOpsRgistry.validate(request, true, VCodeConstants.ATTR_KEY_VCODE_ROBOT_FORBIDDEN);
				if(result.getStatus()==1)
					result = pwdLogin(request, username, password);
			}
		}
		ModelAndView mv = createModelAndView(request, result, redirectTo);
		return mv;
	}

	private Result pwdLogin(HttpServletRequest request, String username, String _password) throws Exception {
		String password = CommonHelper.trim(_password);
		Result result = password!=null?login(request, username, password):new Result(0, new RequestContext(request).getMessage("auth.null.password"));
		HttpSession session = request.getSession();//如果doPwdLogin没登录成功，session是空的，需要创建session用来保存失败次数，但是session失效后失败次数也随之消失，还是可以继续重试
		if(result.getStatus()==LOGIN_STATUS_PWD_INCORRECT) {
			int failuresPp = this.getFailures(request)+1;
			session.setAttribute(LOGIN_FAILURES_HOLDER_KEY, failuresPp);
			if(failuresPp>=failuresThresholdForVCode)
				result.setStatus(LOGIN_STATUS_PWD_INCORRECT-1);
		} else if(result.getStatus()==1)
			session.removeAttribute(LOGIN_FAILURES_HOLDER_KEY);
		return result;
	}
	/**
	 * 临时密码登录
	 */
	@RequestMapping("/signin/vcode")
	public ModelAndView vcodeLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		Result result = vCodeOpsRgistry.validate(request, true, VCodeConstants.ATTR_KEY_VCODE_LOGIN);
		if(result.getStatus()==1) {
			HttpSession session = request.getSession(false);
			result = login(request, session.getAttribute(PARA_NAME_USERNAME).toString(), null);
		}
		return createModelAndView(request, result, redirectTo);
	}
	/**
	 * 生成临时密码并发送至邮箱
	 */
	@RequestMapping("/vcode/email")
	public ModelAndView vcodeToEmail(HttpServletRequest request, @RequestParam(required = true, name = PARA_NAME_USERNAME)String _email) throws Exception {
		Assert.notNull(tempPwdLoginEmailInfo, "没有设置邮件标题和内容模板");
		Integer status = null;
		String msgI18N = null;
		String email = CommonHelper.trim(_email);
		if(email==null) {
			status = 0;
			msgI18N = "email.null";
		} else {
			if(!CommonHelper.isEmail(email)) {
				status = -1;
				msgI18N = "email.illegal";
			} else {
				status = 1;
				msgI18N = Result.I18N_KEY_SUCCESS;
				vCodeOpsRgistry.genAndSend(request, VCodeCharsFrom.MIXED, 32, email, tempPwdLoginEmailInfo.getSubject(), tempPwdLoginEmailInfo.getContent());
			}
		}
		return InnerHelper.modelAndViewI18N(request, status, msgI18N);
	}
}