package com.quincy.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.core.VCodeConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.AuthActions;
import com.quincy.sdk.Result;
import com.quincy.sdk.TempPwdLoginEmailInfo;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeOpsRgistry;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthorizationServerController extends BaseAuthServerController {
	@Autowired(required = false)
	private AuthActions authActions;
	@Autowired
	private VCodeOpsRgistry vCodeOpsRgistry;
	@Autowired(required = false)
	private TempPwdLoginEmailInfo tempPwdLoginEmailInfo;
	@Value("${auth.tmppwd.length:32}")
	private int tmppwdLength;
	private final static String PARA_NAME_USERNAME = "username";
	/**
	 * 进登录页
	 */
	@RequestMapping("/signin")
	public ModelAndView toLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo) {
		Assert.notNull(authActions, AUTH_ACTIONS_NULL_MSG);
		ModelAndView mv = new ModelAndView("/login");
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
		return InnerHelper.modelAndViewResult(request, result, redirectTo!=null?"redirect:"+redirectTo:null);
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
			result = login(request, session.getAttribute(PARA_NAME_USERNAME).toString());
		}
		return InnerHelper.modelAndViewResult(request, result, redirectTo!=null?"redirect:"+redirectTo:null);
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
				vCodeOpsRgistry.genAndSend(request, VCodeCharsFrom.MIXED, tmppwdLength, email, tempPwdLoginEmailInfo.getSubject(), tempPwdLoginEmailInfo.getContent());
			}
		}
		return InnerHelper.modelAndViewI18N(request, status, msgI18N);
	}
}