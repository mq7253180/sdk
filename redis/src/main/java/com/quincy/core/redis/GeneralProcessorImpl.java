package com.quincy.core.redis;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
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
import com.quincy.sdk.EmailService;
import com.quincy.sdk.RedisOperation;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.RedisWebOperation;
import com.quincy.sdk.VCcodeSender;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.annotation.VCodeRequired;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;

import redis.clients.jedis.Jedis;

@Component
public class GeneralProcessorImpl extends HandlerInterceptorAdapter implements RedisProcessor {
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
				String inputedVCode = CommonHelper.trim(request.getParameter(InnerConstants.ATTR_VCODE));
				Integer status = null;
				String msgI18NKey = null;
				if(inputedVCode==null) {
					status = -5;
					msgI18NKey = "vcode.null";
				} else {
					String cachedVCode = CommonHelper.trim(this.getCachedVCode(request));
					if(cachedVCode==null) {
						status = -6;
						msgI18NKey = "vcode.expire";
					} else if(!cachedVCode.equalsIgnoreCase(inputedVCode)) {
						status = -7;
						msgI18NKey = "vcode.not_matched";
					}
				}
				if(status==null) {
					this.rmCachedStr(request, FLAG_VCODE);
				} else {
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
	public Object opt(RedisOperation operation) throws Exception {
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			return operation.run(jedis);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	@Override
	public Object opt(HttpServletRequest request, RedisWebOperation operation) throws Exception {
		String clientTokenName = CommonHelper.trim(properties.getProperty(InnerConstants.CLIENT_TOKEN_PROPERTY_NAME));
		String token = CommonHelper.getValue(request, clientTokenName);
		if(token!=null) {
			return this.opt(new RedisOperation() {
				@Override
				public Object run(Jedis jedis) throws Exception {
					return operation.run(jedis, token);
				}
			});
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
			jedis.expire(key, Integer.parseInt(properties.getProperty("vcode.expire"))*60);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	private String getCachedStr(HttpServletRequest request, String flag) throws Exception {
		Object retVal = this.opt(request, new RedisWebOperation() {
			@Override
			public Object run(Jedis jedis, String token) {
				String key = combineAsKey(flag, token);
				String vcode = jedis.get(key);
				return vcode;
			}
		});
		return retVal==null?null:String.valueOf(retVal);
	}

	private void rmCachedStr(HttpServletRequest request, String flag) {
		String key = combineAsKey(flag, this.createOrGetToken(request));
		Jedis jedis = null;
		try {
			jedis = jedisSource.get();
			jedis.del(key);
		} finally {
			if(jedis!=null)
				jedis.close();
		}
	}

	private String combineAsKey(String flag, String token) {
		return appName+"."+flag+"."+token;
	}

	@Override
	public String createOrGetToken(HttpServletRequest request) {
		String clientTokenName = CommonHelper.trim(properties.getProperty(InnerConstants.CLIENT_TOKEN_PROPERTY_NAME));
		String token = CommonHelper.getValue(request, clientTokenName);
		if(token==null) {
			token = UUID.randomUUID().toString().replaceAll("-", "");
			Cookie cookie = new Cookie(clientTokenName, token);
			cookie.setDomain(CommonHelper.trim(properties.getProperty("domain")));
			cookie.setPath("/");
			cookie.setMaxAge(3600*12);
			HttpServletResponse response = CommonHelper.getResponse();
			response.addCookie(cookie);
		}
		return token;
	}

	private void cacheVCode(HttpServletRequest request, String vcode) {
		this.cacheStr(request, FLAG_VCODE, vcode);
	}

	private String getCachedVCode(HttpServletRequest request) throws Exception {
		return this.getCachedStr(request, FLAG_VCODE);
	}

	@Override
	public char[] vcode(HttpServletRequest request, VCodeCharsFrom _charsFrom, VCcodeSender sender) throws Exception {
		String charsFrom = (_charsFrom==null?VCodeCharsFrom.MIXED:_charsFrom).getValue();
		int length = Integer.parseInt(properties.getProperty("vcode.length"));
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		char[] _vcode = new char[length];
		for(int i=0;i<length;i++) {
			char c = charsFrom.charAt(random.nextInt(charsFrom.length()));
			sb.append(c);
			_vcode[i] = c;
		}
		String vcode = sb.toString();
		this.cacheVCode(request, vcode);
		sender.send(_vcode);
		return _vcode;
	}

	private final double radians = Math.PI/180;

	@Override
	public char[] vcode(HttpServletRequest request, HttpServletResponse response, VCodeCharsFrom charsFrom, int size, int start, int space, int width, int height) throws Exception {
		return this.vcode(request, charsFrom, new VCcodeSender() {
			@Override
			public void send(char[] vcode) throws IOException {
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
				Graphics g = image.getGraphics();
				Graphics2D gg = (Graphics2D)g;
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);//填充背景
				int lines = Integer.parseInt(properties.getProperty("vcode.lines"));
				Random random = new Random();
				for(int i=0;i<lines;i++) {
					g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
					g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
				}
				Font font = new Font("Times New Roman", Font.ROMAN_BASELINE, size);
				g.setFont(font);
//				g.translate(random.nextInt(3), random.nextInt(3));
		        int x = start;//旋转原点的 x 坐标
				for(char c:vcode) {
		            double tiltAngle = random.nextInt()%30*radians;//角度小于30度
					g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
					gg.rotate(tiltAngle, x, 45);
					g.drawString(c+"", x, size);
		            gg.rotate(-tiltAngle, x, 45);
		            x += space;
				}
				OutputStream out = null;
				try {
					out = response.getOutputStream();
					ImageIO.write(image, "jpg", out);
					out.flush();
				} finally {
					if(out!=null)
						out.close();
				}
			}
		});
	}

	@Autowired
	private EmailService emailService;

	@Override
	public char[] vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, String emailTo, String subject, String _content) throws Exception {
		return this.vcode(request, charsFrom, new VCcodeSender() {
			@Override
			public void send(char[] _vcode) {
				String vcode = new String(_vcode);
				String content = MessageFormat.format(_content, vcode);
				content = String.format(content, vcode);
				emailService.send(emailTo, subject, content, "", null, null, null, null);
			}
		});
	}
}