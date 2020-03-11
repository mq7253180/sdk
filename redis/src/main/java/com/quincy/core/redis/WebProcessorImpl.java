package com.quincy.core.redis;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.RedisOperation;
import com.quincy.sdk.RedisWebProcessor;
import com.quincy.sdk.annotation.VCodeRequired;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

import redis.clients.jedis.Jedis;

@Component
public class WebProcessorImpl extends HandlerInterceptorAdapter implements RedisWebProcessor {
	@Autowired
	private JedisSource jedisSource;
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;
	@Value("${spring.application.name}")
	private String appName;
	private final static String FLAG_VCODE = "vcode";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			VCodeRequired annotation = method.getMethod().getDeclaredAnnotation(VCodeRequired.class);
			if(annotation!=null) {
				String inputedVcode = CommonHelper.trim(request.getParameter(InnerConstants.ATTR_VCODE));
				Integer status = null;
				String msgI18NKey = null;
				if(inputedVcode==null) {
					status = -3;
					msgI18NKey = "vcode.null";
				} else {
					String cachedVcode = CommonHelper.trim(this.getCachedVcode(request));
					if(cachedVcode==null) {
						status = -4;
						msgI18NKey = "vcode.expire";
					} else if(!cachedVcode.equals(inputedVcode)) {
						status = -5;
						msgI18NKey = "vcode.not_matched";
					}
				}
				if(status!=null) {
					RequestContext requestContext = new RequestContext(request);
					String outputContent = "{\"status\":"+status+", \"msg\":\""+requestContext.getMessage(msgI18NKey)+"\"}";
					HttpClientHelper.outputJson(response, outputContent);
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Object opt(HttpServletRequest request, RedisOperation operation) throws Exception {
		String token = CommonHelper.trim(CommonHelper.getValue(request, InnerConstants.CLIENT_TOKEN));
		if(token!=null) {
			Jedis jedis = null;
			try {
				jedis = jedisSource.get();
				return operation.run(jedis, token);
			} finally {
				if(jedis!=null)
					jedis.close();
			}
		}
		return null;
	}

	private void cacheStr(HttpServletRequest request, String flag, String content) {
		String token = this.createOrGetToken(request);
		String key = combineAsKey(flag, token);
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			jedis.set(key, content);
			jedis.expire(key, Integer.parseInt(properties.getProperty("expire.vcode"))*60);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	private String getCachedStr(HttpServletRequest request, String flag) throws Exception {
		Object retVal = this.opt(request, new RedisOperation() {
			@Override
			public Object run(Jedis jedis, String token) {
				String key = combineAsKey(flag, token);
				String vcode = jedis.get(key);
				return vcode;
			}
		});
		return retVal==null?null:String.valueOf(retVal);
	}

	private String combineAsKey(String flag, String token) {
		return appName+"."+flag+"."+token;
	}

	@Override
	public String createOrGetToken(HttpServletRequest request) {
		String token = CommonHelper.trim(CommonHelper.getValue(request, InnerConstants.CLIENT_TOKEN));
		if(token==null) {
			token = UUID.randomUUID().toString().replaceAll("-", "");
			Cookie cookie = new Cookie(InnerConstants.CLIENT_TOKEN, token);
			cookie.setDomain(CommonHelper.trim(properties.getProperty("domain")));
			cookie.setPath("/");
			cookie.setMaxAge(3600*12);
			HttpServletResponse response = CommonHelper.getResponse();
			response.addCookie(cookie);
		}
		return token;
	}

	private void cacheVcode(HttpServletRequest request, String vcode) {
		this.cacheStr(request, FLAG_VCODE, vcode);
	}

	private String getCachedVcode(HttpServletRequest request) throws Exception {
		return this.getCachedStr(request, FLAG_VCODE);
	}

	private final static int width = 88;
	private final static int height = 40;
	private final static int lines = 5;
	
	private final static String VCODE_COMBINATION_FROM = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";

	@Override
	public void vcode(HttpServletRequest request, HttpServletResponse response, int length) throws IOException {
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		for(int i=0;i<length;i++)
			sb.append(VCODE_COMBINATION_FROM.charAt(random.nextInt(VCODE_COMBINATION_FROM.length())));
		String vcode = sb.toString();
		this.cacheVcode(request, vcode);
		OutputStream out = null;
		try {
			out = response.getOutputStream();
			this.drawAsByteArray(vcode, random, out);
			out.flush();
		} finally {
			if(out!=null)
				out.close();
		}
	}

	private void drawAsByteArray(String arg, Random random, OutputStream output) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		Graphics g = image.getGraphics();
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
		for(int i=0;i<lines;i++)
			g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
		Font font = new Font("Times New Roman", Font.ROMAN_BASELINE, 25);
		g.setFont(font);
		g.setColor(new Color(random.nextInt(101), random.nextInt(111), random.nextInt(121)));
//		g.translate(random.nextInt(3), random.nextInt(3));
		g.drawString(arg, 13, 25);
		ImageIO.write(image, "jpg", output);
	}
}