package com.quincy.core.redis;

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

import com.quincy.auth.controller.AuthActions;
import com.quincy.auth.o.User;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.Client;
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
@RequestMapping("/auth")
public class AuthServerController {
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
	private AuthActions authActions;
	private final static String URI_VCODE_PWDSET_SIGNIN = "/pwdset";

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
						.append("/auth")
						.append(URI_VCODE_PWDSET_SIGNIN)
						.append("?token=")
						.append(token)
						.append("&vcode=")
						.append(vcode)
						.toString();
				User user = authActions.findUser(email, Client.get(request));
				if(user==null) {
					status = -11;
					msgI18N = "auth.pwdreset.link.nouser";
				} else {
					String key = keyPrefix+"tmppwd:"+token;
					Jedis jedis = null;
					Transaction tx = null;
			    	try {
			    		jedis = jedisSource.get();
			    		tx = jedis.multi();
			    		tx.hset(key, "userId", user.getId().toString());
			    		tx.hset(key, "vcode", vcode);
			    		if(user.getShardingKey()!=null)
			    			tx.hset(key, "shardingKey", user.getShardingKey().toString());
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
		}
		return InnerHelper.modelAndViewI18N(request, status, msgI18N);
	}

	@RequestMapping(URI_VCODE_PWDSET_SIGNIN)
	public ModelAndView signin(HttpServletRequest request, @RequestParam(required = true, name = "token")String token, @RequestParam(required = true, name = "vcode")String vcode) {
    	return InnerHelper.modelAndViewResult(request, this.validate(request, token, vcode), "/password");
	}

	@RequestMapping(URI_VCODE_PWDSET_SIGNIN+"/update")
	public ModelAndView update(HttpServletRequest request, @RequestParam(required = true, name = "token")String token, @RequestParam(required = true, name = "vcode")String vcode, @RequestParam(required = true, name = "password")String password) {
		Result result = this.validate(null, password, password);
		if(result.getStatus()==1) {
			String[] data = result.getData().toString().split("_");
			User user = new User();
			user.setId(Long.valueOf(data[0]));
			user.setPassword(password);
			if(data.length>1)
				user.setShardingKey(Integer.valueOf(data[1]));
			authActions.updatePassword(user);
		}
		return InnerHelper.modelAndViewResult(request, result);
	}

	private Result validate(HttpServletRequest request, String token, String vcode) {
		String key = keyPrefix+"tmppwd:"+token;
		Jedis jedis = null;
		String userId = null;
		String password = null;
		String shardingKey = null;
    	try {
    		jedis = jedisSource.get();
    		userId = jedis.hget(key, "userId");
    		password = jedis.hget(key, "vcode");
    		shardingKey = jedis.hget(key, "shardingKey");
    	} finally {
    		if(jedis!=null)
    			jedis.close();
    	}
    	Integer status = null;
    	String i18nKey = null;
    	if(userId==null||password==null) {
    		status = -9;
    		i18nKey = "auth.pwdreset.link.timeout";
    	} else if(!password.equals(vcode)) {
    		status = -10;
    		i18nKey = "auth.pwdreset.link.invalid";
    	} else {
    		status = 1;
    		i18nKey = Result.I18N_KEY_SUCCESS;
    	}
    	String data = userId;
    	if(shardingKey!=null) {
    		data += "_";
    		data += shardingKey;
    	}
    	return new Result(status, new RequestContext(request).getMessage(i18nKey), data);
	}
}