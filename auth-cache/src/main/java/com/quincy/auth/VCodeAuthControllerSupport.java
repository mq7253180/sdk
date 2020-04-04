package com.quincy.auth;

import java.net.URLEncoder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.controller.AuthorizationControllerSupport;
import com.quincy.core.InnerConstants;
import com.quincy.core.RedisInnerConstants;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.annotation.JedisInjector;
import com.quincy.sdk.annotation.VCodeRequired;

import redis.clients.jedis.Jedis;

public abstract class VCodeAuthControllerSupport extends AuthorizationControllerSupport {
	@Autowired
	private RedisProcessor redisProcessor;
	@Resource(name = "loginFailuresHolderKey")
	private String loginFailuresHolderKey;
	@Value("${vcode.loginFailures}")
	private int failuresThresholdForVCode;
	/**
	 * 密码登录
	 */
	@JedisInjector
	@PostMapping("/signin/pwd")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = AuthConstants.PARA_NAME_PASSWORD)String password, 
			@RequestParam(required = false, value = "vcode")String vcode, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo, 
			Jedis jedis) throws Exception {
		Result result = null;
		if(failuresThresholdForVCode>=Integer.MAX_VALUE) {
			result = login(request, username, password, null, jedis);
		} else {
			String _failures = jedis.hget(loginFailuresHolderKey, username);
			int failures = _failures==null?0:Integer.parseInt(_failures);
			if(failures<failuresThresholdForVCode) {
				result = login(request, username, password, failures, jedis);
			} else {
				result = redisProcessor.validateVCode(request, null, true);
				if(result.getStatus()==1)
					result = login(request, username, password, failures, jedis);
			}
		}
		if(result.getStatus()==1)
			jedis.hdel(loginFailuresHolderKey, username);
		ModelAndView mv = createModelAndView(request, result, redirectTo);
		return mv;
	}

	private Result login(HttpServletRequest request, String username, String password, Integer failures, Jedis jedis) throws Exception {
		Result result = doPwdLogin(request, username, password);
		if(failures!=null&&result.getStatus()==AuthConstants.LOGIN_STATUS_PWD_INCORRECT) {
			jedis.hincrBy(loginFailuresHolderKey, username, 1);
			if(failures+1>=failuresThresholdForVCode)
				result.setStatus(AuthConstants.LOGIN_STATUS_PWD_INCORRECT-1);
		}
		return result;
	}
	/**
	 * 验证码登录
	 */
	@VCodeRequired
	@RequestMapping("/signin/vcode")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		Result result = login(request, username, null);
		return createModelAndView(request, result, redirectTo);
	}

	@VCodeRequired(clientTokenName = AuthConstants.PARA_NAME_USERNAME)
	@RequestMapping("/signin/vcode/x")
	public ModelAndView vcodeLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = AuthConstants.PARA_NAME_USERNAME)String username, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		return this.doLogin(request, username, redirectTo);
	}

	private final static String PWDSET_CLIENT_TOKEN_NAME = "email";

	@VCodeRequired(clientTokenName = PWDSET_CLIENT_TOKEN_NAME, timeoutForwardTo = "/auth"+RedisInnerConstants.URI_VCODE_PWDSET_TIMEOUT)
	@RequestMapping(RedisInnerConstants.URI_VCODE_PWDSET_SIGNIN)
	public ModelAndView doLoginAsPwdReset(HttpServletRequest request, 
			@RequestParam(required = false, value = PWDSET_CLIENT_TOKEN_NAME)String email, 
			@RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String redirectTo) throws Exception {
		return this.doLogin(request, email, redirectTo);
	}

	@RequestMapping(RedisInnerConstants.URI_VCODE_PWDSET_TIMEOUT)
	public String pwdResetTimeout() {
		return "/pwdset_timeout";
	}

	@RequestMapping(RedisInnerConstants.URI_VCODE_FAILURE)
	public String vcodeFailure(HttpServletRequest request) {
		return InnerConstants.VIEW_PATH_RESULT;
	}

	@Value("${clientTokenName}")
	private String clientTokenName;
	protected abstract String getPwdSetEmailSubject();
	protected abstract String getPwdSetEmailContent(String uri);

	@RequestMapping("/vcode/pwdset")
	@ResponseBody
	public void vcode(HttpServletRequest request, @RequestParam(required = true, name = "email")String email) throws Exception {
		String uri = new StringBuilder(100)
				.append("/auth")
				.append(RedisInnerConstants.URI_VCODE_PWDSET_SIGNIN)
				.append("?")
				.append(PWDSET_CLIENT_TOKEN_NAME)
				.append("=")
				.append(URLEncoder.encode(email, "UTF-8"))
				.append("&vcode={0}&")
				.append(InnerConstants.PARAM_REDIRECT_TO)
				.append("=")
				.append(URLEncoder.encode("/auth"+URI_PWD_SET, "UTF-8"))
				.toString();
		redisProcessor.vcode(request, VCodeCharsFrom.MIXED, 32, "email", email, getPwdSetEmailSubject(), getPwdSetEmailContent(uri));
	}
}