package com.quincy.auth.controller;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.core.VCodeConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quincy.auth.AuthConstants;
import com.quincy.auth.SessionInvalidation;
import com.quincy.auth.entity.UserEntity;
import com.quincy.auth.service.UserService;
import com.quincy.auth.service.XSessionService;
import com.quincy.core.InnerConstants;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.AuthActions;
import com.quincy.sdk.Client;
import com.quincy.sdk.Result;
import com.quincy.sdk.TempPwdLoginEmailInfo;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeOpsRgistry;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.o.User;
import com.quincy.sdk.o.XSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthorizationServerController {
	@Autowired
	private VCodeOpsRgistry vCodeOpsRgistry;
	@Autowired(required = false)
	private TempPwdLoginEmailInfo tempPwdLoginEmailInfo;
	@Value("${auth.tmppwd.length:32}")
	private int tmppwdLength;
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
	private final static String PARA_NAME_USERNAME = "username";
	private final static String SESSION_ATTR_NAME_USERID = "userid";
	private final static String SESSION_ATTR_NAME_LOGINNAME = "loginname";
	protected final static int LOGIN_STATUS_PWD_INCORRECT = -3;
	/**
	 * 进登录页
	 */
	@RequestMapping("/signin")
	public ModelAndView toLogin(HttpServletRequest request, @RequestParam(required = false, value = InnerConstants.PARAM_REDIRECT_TO)String _redirectTo) {
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
		RequestContext requestContext = new RequestContext(request);
		Result result = this.validate(request, username);
		if(result.getStatus()<1) {
			result.setMsg(requestContext.getMessage(result.getMsg()));
			return result;
		}
		String password = CommonHelper.trim(_password);
		result = password!=null?login(request, Long.valueOf(result.getData().toString()), password, username):new Result(0, requestContext.getMessage("auth.null.password"));
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
			Object userId = session.getAttribute(SESSION_ATTR_NAME_USERID);
			result = userId==null?new Result(-9, new RequestContext(request).getMessage("auth.account.vcode_no")):login(request, Long.valueOf(userId.toString()), session.getAttribute(SESSION_ATTR_NAME_LOGINNAME).toString());
		}
		return InnerHelper.modelAndViewResult(request, result, redirectTo!=null?"redirect:"+redirectTo:null);
	}
	/**
	 * 生成临时密码并发送至邮箱
	 */
	@RequestMapping("/vcode/email")
	public ModelAndView vcodeToEmail(HttpServletRequest request, @RequestParam(required = true, name = PARA_NAME_USERNAME)String email) throws Exception {
		Assert.notNull(tempPwdLoginEmailInfo, "没有设置邮件标题和内容模板");
		return this.sendVCode(request, email, "email", new Sender() {
			@Override
			public boolean validate(String username) {
				return CommonHelper.isEmail(username);
			}

			@Override
			public void send(String username) throws Exception {
				vCodeOpsRgistry.genAndSend(request, VCodeCharsFrom.MIXED, tmppwdLength, username, tempPwdLoginEmailInfo.getSubject(), tempPwdLoginEmailInfo.getContent());
			}
		});
	}

	private ModelAndView sendVCode(HttpServletRequest request, String _username, String i18nPrefix, Sender sender) throws Exception {
		Integer status = null;
		String msgI18N = null;
		String username = CommonHelper.trim(_username);
		if(username==null) {
			status = 0;
			msgI18N = i18nPrefix+".null";
		} else {
			if(!sender.validate(username)) {
				status = -3;
				msgI18N = i18nPrefix+".illegal";
			} else {
				Result result = this.validate(request, username);
				if(result.getStatus()<1) {
					status = result.getStatus();
					msgI18N = result.getMsg();
				} else {
					status = 1;
					msgI18N = Result.I18N_KEY_SUCCESS;
					HttpSession session = request.getSession();
					session.setAttribute(SESSION_ATTR_NAME_USERID, result.getData());
					session.setAttribute(SESSION_ATTR_NAME_LOGINNAME, username);
					sender.send(username);
				}
			}
		}
		return InnerHelper.modelAndViewI18N(request, status, msgI18N);
	}

	private static interface Sender {
		public boolean validate(String username);
		public void send(String username) throws Exception;
	}

	protected Result validate(HttpServletRequest request, String _username) {
		Result result = new Result();
		String username = CommonHelper.trim(_username);
		if(username==null) {
			result.setStatus(-1);
			result.setMsg("auth.null.username");
			return result;
		}
		Long userId = userService.findUserId(username);
		if(userId==null) {
			result.setStatus(-2);
			result.setMsg("auth.account.no");
			return result;
		}
		result.setStatus(1);
		result.setData(userId);
		return result;
	}

	private Result login(HttpServletRequest request, Long userId, String loginName) throws Exception {
		return this.login(request, userId, null, loginName);
	}

	private Result login(HttpServletRequest request, Long userId, String password, String loginName) throws Exception {
		RequestContext requestContext = new RequestContext(request);
		Client client = Client.get(request);
		XSession xsession = null;
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
		User user = userService.find(userId, client);
		boolean tourist = false;
		if(user==null) {//游客登录，只有映射关系，还没插入用户表信息
			if(password!=null)//游客还没有设置密码，如果不拦住，可以通过密码登录直接登录进来
				return new Result(-8, requestContext.getMessage("auth.tourist.password"));
			user = new User();
			user.setId(userId);
			user.setName(requestContext.getMessage("auth.tourist"));
			xsession = new XSession();
			tourist = true;
		} else {
			if(password!=null&&!password.equalsIgnoreCase(user.getPassword()))
				return new Result(LOGIN_STATUS_PWD_INCORRECT, requestContext.getMessage("auth.account.pwd_incorrect"));
			if(authActions!=null) {
				Map<String, Serializable> attrs = new HashMap<String, Serializable>();
				user.setAttributes(attrs);
				authActions.onLogin(userId, attrs);
			}
			if(sessionInvalidation!=null) {//同一user同一类端之间互踢，清除session
				String originalJsessionid = CommonHelper.trim(user.getJsessionid());
				if(originalJsessionid!=null&&!originalJsessionid.equals(session.getId())) {
					if((client.isPc()&&sessionInvalidation.pcBrowserEvict())//PC浏览器互踢
							||(client.isMobile()&&sessionInvalidation.mobileBrowserEvict())//移动设备浏览器互踢
							||(client.isApp()&&sessionInvalidation.appEvict())//APP互踢
						) {
						sessionInvalidation.invalidate(originalJsessionid);
						this.updateLastLogin(user.getId(), session.getId(), client);
					}
				}
			}
			user.setJsessionid(session.getId());
			xsession = xSessionService==null?new XSession():xSessionService.create(user);
		}
		if(!tourist) {
			if(CommonHelper.isMobilePhone(loginName)) {
				String mobilePhone = CommonHelper.trim(user.getMobilePhone());
				if(!loginName.equals(mobilePhone)) {//分片事务补偿，如果换邮箱时删除原映射关系、更新用户表邮箱字段失败情况，单库模式在同一事务中不涉及此问题
					user.setMobilePhone(loginName);
					userService.deleteMappingAndUpdateUser(mobilePhone, vo->{vo.setMobilePhone(loginName);}, userId);
				}
			} else if(CommonHelper.isEmail(loginName)) {
				String email = CommonHelper.trim(user.getEmail());
				if(!loginName.equals(email)) {
					user.setEmail(loginName);
					userService.deleteMappingAndUpdateUser(email, vo->{vo.setEmail(loginName);}, userId);
				}
			} else {
				String username = CommonHelper.trim(user.getUsername());
				if(!loginName.equals(username)) {
					user.setUsername(loginName);
					userService.deleteMappingAndUpdateUser(username, vo->{vo.setUsername(loginName);}, userId);
				}
			}
		}
		xsession.setUser(user);
		session.setAttribute(AuthConstants.ATTR_SESSION, xsession);
		Result result = Result.newSuccess();
		result.setMsg(requestContext.getMessage(result.getMsg()));
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

	@RequestMapping("/signup/vcode")
	@ResponseBody
	public Result sendVCode(HttpServletRequest request, @RequestParam(PARA_NAME_USERNAME) String loginName) throws Exception {
		Assert.notNull(authActions, "AuthActions没有实现");
		Long userId = userService.findUserId(loginName);
		Result result = null;
		if(userId!=null) {
			result = new Result(0, "auth.mapping.new");
		} else {
			if(CommonHelper.isEmail(loginName)) {
				vCodeOpsRgistry.genAndSend(request, VCodeCharsFrom.DIGITS, 6, loginName, tempPwdLoginEmailInfo.getSubject(), tempPwdLoginEmailInfo.getContent());
			} else if(CommonHelper.isMobilePhone(loginName)) {
				vCodeOpsRgistry.genAndSend(request, VCodeCharsFrom.DIGITS, 6, (vcode, expireMinuts)->{
					authActions.sms(loginName, new String(vcode), expireMinuts);
				});
			} else
				result = new Result(-1, "auth.signup.valid");
		}
		if(result==null) {
			request.getSession().setAttribute(SESSION_ATTR_NAME_LOGINNAME, loginName);
			result = Result.newSuccess();
		}
		result.setMsg(new RequestContext(request).getMessage(result.getMsg()));
		return result;
	}

	@RequestMapping("/signup")
	@ResponseBody
	public Result signUp(HttpServletRequest request) throws Exception {
		Result result = vCodeOpsRgistry.validate(request, false, VCodeConstants.ATTR_KEY_VCODE_LOGIN, VCodeConstants.ATTR_KEY_VCODE_SIGNUP);
		if(result.getStatus()==1) {
			String loginName = request.getSession(false).getAttribute(SESSION_ATTR_NAME_LOGINNAME).toString();
			Long userId = userService.createMapping(loginName);
			if(userId==null) {
				result = new Result(0, new RequestContext(request).getMessage("auth.mapping.new"));
			} else {
				result = this.login(request, userId, loginName);
				result.setMsg(loginName+"注册成功，userId为"+userId+"，在第"+(loginName.hashCode()%8)+"个分片");
			}
		}
		return result;
	}
	/**
	 * 生成临时密码并发送至短信
	 */
	@RequestMapping("/vcode/sms")
	public ModelAndView vcodeToSms(HttpServletRequest request, @RequestParam(required = true, name = PARA_NAME_USERNAME)String phoneNmumer) throws Exception {
		Assert.notNull(authActions, "AuthActions没有实现");
		return this.sendVCode(request, phoneNmumer, "email", new Sender() {
			@Override
			public boolean validate(String username) {
				return CommonHelper.isMobilePhone(username);
			}

			@Override
			public void send(String username) throws Exception {
				vCodeOpsRgistry.genAndSend(request, VCodeCharsFrom.DIGITS, 6, (vcode, expireMinuts)->{
					authActions.sms(username, new String(vcode), expireMinuts);
				});
			}
		});
	}
}