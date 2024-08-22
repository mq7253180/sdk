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

import com.quincy.core.InnerHelper;
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
	@Autowired
	private VCodeOpsRgistry vCodeOpsRgistry;
	@Autowired
	private EmailService emailService;
	@Autowired
	@Qualifier(RedisConstants.BEAN_NAME_SYS_JEDIS_SOURCE)
	private JedisSource jedisSource;
	@Autowired(required = false)
	private PwdRestEmailInfo pwdRestEmailInfo;
	private final static String URI_VCODE_PWDSET_SIGNIN = "/pwdset/signin";

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
				String vcode = new String(vCodeOpsRgistry.generate(VCodeCharsFrom.MIXED, 16));
				String uri = new StringBuilder(200)
						.append(authCenter)
						.append("/auth")
						.append(URI_VCODE_PWDSET_SIGNIN)
						.append("?token=")
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

	@RequestMapping(URI_VCODE_PWDSET_SIGNIN)
	public ModelAndView signin() {
		return null;
	}
}