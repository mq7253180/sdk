package com.quincy.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.controller.AuthorizationControllerSupport;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.Result;
import com.quincy.sdk.annotation.JedisInjector;
import com.quincy.sdk.annotation.VCodeRequired;

import redis.clients.jedis.Jedis;

public abstract class VCodeAuthControllerSupport extends AuthorizationControllerSupport {
	@Autowired
	private RedisProcessor redisProcessor;
	@Value("${spring.application.name}")
	private String applicationName;
	private final static String FAILURES_HOLDER_KEY = ".login_failures";
	private final static int MAX_FAILURES_ALLOWED = 3;
	/**
	 * 密码登录
	 */
	@JedisInjector
	@PostMapping("/signin/pwd")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = "username")String username, 
			@RequestParam(required = false, value = "password")String password, 
			@RequestParam(required = false, value = "vcode")String vcode, 
			@RequestParam(required = false, value = AuthConstants.PARAM_BACK_TO)String _backTo, 
			Jedis jedis) throws Exception {
		Result result = null;
		String key = applicationName+FAILURES_HOLDER_KEY;
		String _failures = jedis.hget(key, username);
		long failures = _failures==null?0:Integer.parseInt(_failures);
		if(failures<MAX_FAILURES_ALLOWED) {
			result = this.doPwdLogin(request, username, password, failures, jedis, key);
		} else {
			result = redisProcessor.validateVCode(request);
			if(result.getStatus()==1)
				result = this.doPwdLogin(request, username, password, failures, jedis, key);
		}
		if(result.getStatus()==1)
			jedis.hdel(key, username);
		ModelAndView mv = createModelAndView(request, result, _backTo);
		return mv;
	}

	private Result doPwdLogin(HttpServletRequest request, String username, String password, long failures, Jedis jedis, String key) throws Exception {
		Result result = doPwdLogin(request, username, password);
		if(result.getStatus()==AuthConstants.LOGIN_STATUS_PWD_INCORRECT) {
			jedis.hincrBy(key, username, 1);
			if(failures+1>=MAX_FAILURES_ALLOWED)
				result.setStatus(AuthConstants.LOGIN_STATUS_PWD_INCORRECT-1);
		}
		return result;
	}
	/**
	 * 验证码登录
	 */
	@VCodeRequired
	@PostMapping("/signin/vcode")
	public ModelAndView doLogin(HttpServletRequest request, 
			@RequestParam(required = false, value = "username")String _username, 
			@RequestParam(required = false, value = AuthConstants.PARAM_BACK_TO)String _backTo) throws Exception {
		Result result = this.login(request, _username, null);
		return this.createModelAndView(request, result, _backTo);
	}
}