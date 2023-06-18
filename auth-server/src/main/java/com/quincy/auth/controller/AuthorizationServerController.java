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
import com.quincy.auth.AuthCommonConstants;
import com.quincy.auth.AuthConstants;
import com.quincy.auth.annotation.VCodeRequired;
import com.quincy.auth.o.XSession;
import com.quincy.auth.o.User;
import com.quincy.auth.service.AuthCallback;
import com.quincy.auth.service.AuthorizationServerService;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
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

	private Result doPwdLogin(HttpServletRequest request, String username, String _password) throws Exception {
		String password = CommonHelper.trim(_password);
		Result result = password!=null?login(request, username, password):new Result(0, new RequestContext(request).getMessage("auth.null.password"));
		return result;
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
		XSession session = authorizationServerService.setSession(request, new AuthCallback() {
			@Override
			public void updateLastLogined(String jsessionid) {
				authActions.updateLastLogin(user.getId(), client, jsessionid);
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


	@Autowired(required = false)
	private PwdRestEmailInfo pwdRestEmailInfo;
	@Autowired
	private AuthorizationCommonController authorizationCommonController;
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
				authorizationCommonController.vcode(request, VCodeCharsFrom.MIXED, 32, "email", email, pwdRestEmailInfo.getSubject(), pwdRestEmailInfo.getContent(uri));
			}
		}
		return new ModelAndView(InnerConstants.VIEW_PATH_RESULT)
				.addObject("status", status)
				.addObject("msg", new RequestContext(request).getMessage(msgI18N));
	}

	private int getFailures(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if(session==null)
			return 0;
		Object _failures = session.getAttribute(LOGIN_FAILURES_HOLDER_KEY);
		return _failures==null?0:Integer.parseInt(_failures.toString());
	}

	@Value("${vcode.loginFailures}")
	private int failuresThresholdForVCode;
	private final static String LOGIN_FAILURES_HOLDER_KEY = "login_failures";
	/**
	 * 密码登录
	 */
	@PostMapping("/signin/pwd")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_PASSWORD)String password, 
			@RequestParam(required = false, value = "vcode")String vcode, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo
			) throws Exception {
		Result result = null;
		if(failuresThresholdForVCode>=Integer.MAX_VALUE) {
			result = loginWithFailureLimit(request, username, password);
		} else {
			int failures = this.getFailures(request);
			if(failures<failuresThresholdForVCode) {
				result = loginWithFailureLimit(request, username, password);
			} else {
				result = this.validateVCode(request, true);
				if(result.getStatus()==1)
					result = loginWithFailureLimit(request, username, password);
			}
		}
		ModelAndView mv = createModelAndView(request, result, redirectTo);
		return mv;
	}

	private Result loginWithFailureLimit(HttpServletRequest request, String username, String password) throws Exception {
		Result result = doPwdLogin(request, username, password);
		HttpSession session = request.getSession();//如果doPwdLogin没登录成功，session是空的，需要创建session用来保存失败次数
		if(result.getStatus()==AuthCommonConstants.LOGIN_STATUS_PWD_INCORRECT) {
			int failuresPp = this.getFailures(request)+1;
			session.setAttribute(LOGIN_FAILURES_HOLDER_KEY, failuresPp);
			if(failuresPp>=failuresThresholdForVCode)
				result.setStatus(AuthCommonConstants.LOGIN_STATUS_PWD_INCORRECT-1);
		} else if(result.getStatus()==1)
			session.removeAttribute(LOGIN_FAILURES_HOLDER_KEY);
		return result;
	}
	/**
	 * 验证码登录
	 * 需要改，生成和验证时key需要和防暴破解有区别，否则调生成验证码接口看图获取验证码之后再调这个接口，可以直接登录
	 */
	@VCodeRequired
	@RequestMapping("/signin/vcode")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		Result result = login(request, username, null);
		return createModelAndView(request, result, redirectTo);
	}

	@VCodeRequired(clientTokenName = AuthCommonConstants.PARA_NAME_USERNAME)
	@RequestMapping("/signin/vcode/x")
	public ModelAndView vcodeLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthCommonConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		return this.doLogin(request, username, redirectTo);
	}
	/**
	 * 也需要改
	 * @param request
	 * @param email
	 * @param redirectTo
	 * @return
	 * @throws Exception
	 */
	@VCodeRequired(clientTokenName = PWDSET_CLIENT_TOKEN_NAME, timeoutForwardTo = "/auth"+AuthConstants.URI_VCODE_PWDSET_TIMEOUT)
	@RequestMapping(AuthConstants.URI_VCODE_PWDSET_SIGNIN)
	public ModelAndView doLoginAsPwdReset(HttpServletRequest request, 
			@RequestParam(required = false, value = PWDSET_CLIENT_TOKEN_NAME)String email, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		return this.doLogin(request, email, redirectTo);
	}

	@RequestMapping(AuthConstants.URI_VCODE_PWDSET_TIMEOUT)
	public String pwdResetTimeout() {
		return "/pwdset_timeout";
	}

	@RequestMapping(AuthConstants.URI_VCODE_FAILURE)
	public String vcodeFailure(HttpServletRequest request) {
		return InnerConstants.VIEW_PATH_RESULT;
	}

	public Result validateVCode(HttpServletRequest request, boolean ignoreCase) throws Exception {
		HttpSession session = request.getSession(false);
		String inputedVCode = CommonHelper.trim(request.getParameter(InnerConstants.ATTR_VCODE));
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
				Object _cachedVCode = session.getAttribute(AuthCommonConstants.VCODE_ATTR_KEY);
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
			session.removeAttribute(AuthCommonConstants.VCODE_ATTR_KEY);
			status = 1;
		} else {
			RequestContext requestContext = new RequestContext(request);
			msg = requestContext.getMessage(msgI18NKey);
		}
		return new Result(status, msg);
	}
}