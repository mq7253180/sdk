package com.quincy.auth.controller;

import java.net.URLEncoder;

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
import com.quincy.auth.o.XSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthorizationServerService;
import com.quincy.core.AuthCommonConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.SessionInvalidation;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeService;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthorizationServerController {
	@Autowired
	private AuthorizationServerService authorizationServerService;
	@Autowired(required = false)
	private AuthActions authActions;
	@Autowired(required = false)
	private SessionInvalidation sessionInvalidation;
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
		Client client = CommonHelper.getClient(request);
		User user = authActions.findUser(username, client);
		if(user==null) {
			result.setStatus(-2);
			result.setMsg(requestContext.getMessage("auth.account.no"));
			return result;
		}
		if(password!=null&&!password.equalsIgnoreCase(user.getPassword())) {
			result.setStatus(AuthCommonConstants.LOGIN_STATUS_PWD_INCORRECT);
			result.setMsg(requestContext.getMessage("auth.account.pwd_incorrect"));
			return result;
		}
		HttpSession session = request.getSession();//所有身份认证通过，创建session
		/*
		 * 验证码登录临时保存验证码时会给session设置一个较短的超时时间，登录成功后在这里给恢复回来
		 */
		//Integer.parseInt(String.valueOf(Duration.parse(sessionTimeoutApp).getSeconds()))
		Object maxInactiveInterval = session.getAttribute(AuthCommonConstants.ATTR_KEY_VCODE_ORIGINAL_MXA_INACTIVE_INTERVAL);
		if(maxInactiveInterval!=null)
			session.setMaxInactiveInterval(Integer.parseInt(maxInactiveInterval.toString()));
		String originalJsessionid = user.getJsessionid();
		user.setJsessionid(session.getId());
		XSession xsession = authorizationServerService.createXSession(user.getId());
		xsession.setUser(user);
		session.setAttribute(InnerConstants.ATTR_SESSION, xsession);
		if(sessionInvalidation!=null) {//同一user不同客户端登录互踢，清除session
			originalJsessionid = CommonHelper.trim(originalJsessionid);
			if(originalJsessionid!=null)
				sessionInvalidation.invalidate(originalJsessionid);
		}
		authActions.updateLastLogin(user.getId(), session.getId());//互踢还需要应用层配合更新数据库里的jsessionid
		result.setStatus(1);
		result.setMsg(requestContext.getMessage("auth.success"));
		result.setData(xsession);
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
				mv = new ModelAndView("redirect:"+(redirectTo!=null?redirectTo:""));
			} else
				mv = createModelAndView(result);
		}
		return mv;
	}

	private ModelAndView createModelAndView(Result result) throws JsonProcessingException {
		return new ModelAndView("/result_login")
				.addObject("status", result.getStatus())
				.addObject("msg", result.getMsg())
				.addObject("data", new ObjectMapper().writeValueAsString(result.getData()));
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
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_PASSWORD)String password, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_VCODE)String vcode, 
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
				result = this.validateVCode(request, true, AuthCommonConstants.ATTR_KEY_VCODE_PWD_LOGIN);
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
		if(result.getStatus()==AuthCommonConstants.LOGIN_STATUS_PWD_INCORRECT) {
			int failuresPp = this.getFailures(request)+1;
			session.setAttribute(LOGIN_FAILURES_HOLDER_KEY, failuresPp);
			if(failuresPp>=failuresThresholdForVCode)
				result.setStatus(AuthCommonConstants.LOGIN_STATUS_PWD_INCORRECT-1);
		} else if(result.getStatus()==1)
			session.removeAttribute(LOGIN_FAILURES_HOLDER_KEY);
		return result;
	}

	public Result validateVCode(HttpServletRequest request, boolean ignoreCase, String attrKey) throws Exception {
		HttpSession session = request.getSession(false);
		String inputedVCode = CommonHelper.trim(request.getParameter(AuthCommonConstants.PARA_NAME_VCODE));
		Integer status = null;
		String msgI18NKey = null;
		String msg = null;
		if(inputedVCode==null) {
			status = -5;
			msgI18NKey = "vcode.null";
		} else {
			if(session==null) {
				status = -6;
				msgI18NKey = "vcode.expire";
			} else {
				Object _cachedVCode = session.getAttribute(attrKey);
				String cachedVCode = _cachedVCode==null?null:CommonHelper.trim(_cachedVCode.toString());
				if(cachedVCode==null) {
					status = -6;
					msgI18NKey = "vcode.expire";
				} else if(!(ignoreCase?cachedVCode.equalsIgnoreCase(inputedVCode):cachedVCode.equals(inputedVCode))) {
					status = -7;
					msgI18NKey = "vcode.not_matched";
				}
			}
		}
		if(status==null) {
			session.removeAttribute(attrKey);
			status = 1;
		} else {
			RequestContext requestContext = new RequestContext(request);
			msg = requestContext.getMessage(msgI18NKey);
		}
		return new Result(status, msg);
	}
	/**
	 * 临时密码登录
	 */
	@RequestMapping("/signin/vcode")
	public ModelAndView vcodeLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		Result result = this.validateVCode(request, true,AuthCommonConstants.ATTR_KEY_VCODE_LOGIN);
		if(result.getStatus()==1) {
			HttpSession session = request.getSession(false);
			result = login(request, session.getAttribute(AuthCommonConstants.ATTR_KEY_USERNAME).toString(), null);
		}
		return createModelAndView(request, result, redirectTo);
	}

	@Autowired
	private VCodeService vCodeService;

	@RequestMapping("/vcode/email")
	public ModelAndView vcodeToEmail(HttpServletRequest request, @RequestParam(required = true, name = AuthCommonConstants.PARA_NAME_USERNAME)String _email) throws Exception {
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
				vCodeService.vcode(request, VCodeCharsFrom.MIXED, 32, email, "", "");
			}
		}
		return new ModelAndView(InnerConstants.VIEW_PATH_RESULT)
				.addObject("status", status)
				.addObject("msg", new RequestContext(request).getMessage(msgI18N));
	}

	@Autowired(required = false)
	private PwdRestEmailInfo pwdRestEmailInfo;
	private final static String PWDSET_CLIENT_TOKEN_NAME = "email";

	@RequestMapping("/vcode/pwdset")
	public ModelAndView vcode(HttpServletRequest request, @RequestParam(required = true, name = "email")String _email) throws Exception {
		Assert.notNull(pwdRestEmailInfo, "没有设置邮件标题和内容模板");
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
				String uri = new StringBuilder(100)
						.append("/auth")
						.append(AuthConstants.URI_VCODE_PWDSET_SIGNIN)
						.append("?")
						.append(PWDSET_CLIENT_TOKEN_NAME)
						.append("=")
						.append(URLEncoder.encode(email, "UTF-8"))
						.append("&vcode={0}&")
						.append(InnerConstants.PARAM_REDIRECT_TO)
						.append("=")
						.append(URLEncoder.encode(AuthConstants.URI_PWD_SET, "UTF-8"))
						.toString();
				vCodeService.vcode(request, VCodeCharsFrom.MIXED, 32, email, pwdRestEmailInfo.getSubject(), pwdRestEmailInfo.getContent(uri));
			}
		}
		return new ModelAndView(InnerConstants.VIEW_PATH_RESULT)
				.addObject("status", status)
				.addObject("msg", new RequestContext(request).getMessage(msgI18N));
	}

	@RequestMapping(AuthConstants.URI_VCODE_PWDSET_TIMEOUT)
	public String pwdResetTimeout() {
		return "/pwdset_timeout";
	}

	@RequestMapping(AuthConstants.URI_VCODE_FAILURE)
	public String vcodeFailure(HttpServletRequest request) {
		return InnerConstants.VIEW_PATH_RESULT;
	}
}