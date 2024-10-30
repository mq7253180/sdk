package com.quincy.core;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.service.UserService;
import com.quincy.core.redis.JedisSource;
import com.quincy.core.redis.RedisConstants;
import com.quincy.sdk.EmailService;
import com.quincy.sdk.PwdRestEmailInfo;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeOpsRgistry;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

@Controller
@RequestMapping("/user")
public class UserRedisController {
	@Value("${auth.center:}")
	private String authCenter;
	@Value("${spring.redis.key.prefix}")
	private String keyPrefix;
	@Value("${auth.vcode.timeout:180}")
	private int vcodeTimeoutSeconds;
	@Value("${auth.tmppwd.length:32}")
	private int tmppwdLength;
	@Autowired
	private VCodeOpsRgistry vCodeOpsRgistry;
	@Autowired
	private EmailService emailService;
	@Autowired
	@Qualifier(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Autowired(required = false)
	private PwdRestEmailInfo pwdRestEmailInfo;
	@Autowired
	private UserService userService;
//	private final static String URI_VCODE_PWDSET_SIGNIN = "/pwdset";

	@RequestMapping("/pwdset/vcode")
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
				String token = UUID.randomUUID().toString();
				String vcode = new String(vCodeOpsRgistry.generate(VCodeCharsFrom.MIXED, tmppwdLength));
				String uri = new StringBuilder(200)
						.append(authCenter)
						.append("/auth/pwdset?token=")
						.append(token)
						.append("&vcode=")
						.append(vcode)
						.toString();
				String key = keyPrefix+"tmppwd:"+token;
				Jedis jedis = null;
				Transaction tx = null;
		    	try {
		    		jedis = jedisSource.get();
		    		tx = jedis.multi();
		    		tx.hset(key, "email", email);
		    		tx.hset(key, "vcode", vcode);
		    		tx.expire(key, vcodeTimeoutSeconds);
		    		tx.exec();
		    	} catch(Exception e) {
		    		tx.discard();
		    		throw e;
		    	} finally {
		    		if(tx!=null)
		    			tx.close();
		    		if(jedis!=null)
		    			jedis.close();
		    	}
				emailService.send(email, pwdRestEmailInfo.getSubject(), pwdRestEmailInfo.getContent(uri, vcodeTimeoutSeconds));
			}
		}
		return InnerHelper.modelAndViewI18N(request, status, msgI18N);
	}

	@RequestMapping("/pwdset/vcode/update")
	public ModelAndView update(HttpServletRequest request, @RequestParam(required = true, name = "token")String token, @RequestParam(required = true, name = "vcode")String vcode, @RequestParam(required = true, name = "password")String password) {
		Result result = this.validate(null, password, password);
		if(result.getStatus()==1) {
			Long userId = userService.findUserId(result.getData().toString());
			if(userId==null) {
				result = new Result(-11, "auth.pwdreset.link.nouser");
			} else {
				userService.updatePassword(userId, password);
				result = Result.newSuccess();
			}
			result.setMsg(new RequestContext(request).getMessage(result.getMsg()));
		}
		return InnerHelper.modelAndViewResult(request, result);
	}

	private Result validate(HttpServletRequest request, String token, String vcode) {
		String key = keyPrefix+"tmppwd:"+token;
		Jedis jedis = null;
		String email = null;
		String password = null;
    	try {
    		jedis = jedisSource.get();
    		email = jedis.hget(key, "email");
    		password = jedis.hget(key, "vcode");
    	} finally {
    		if(jedis!=null)
    			jedis.close();
    	}
    	Integer status = null;
    	String i18nKey = null;
    	if(email==null||password==null) {
    		status = -9;
    		i18nKey = "auth.pwdreset.link.timeout";
    	} else if(!password.equals(vcode)) {
    		status = -10;
    		i18nKey = "auth.pwdreset.link.invalid";
    	} else {
    		status = 1;
    		i18nKey = Result.I18N_KEY_SUCCESS;
    	}
    	return new Result(status, new RequestContext(request).getMessage(i18nKey), email);
	}
}