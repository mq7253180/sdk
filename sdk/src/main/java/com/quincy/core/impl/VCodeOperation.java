package com.quincy.core.impl;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.core.VCodeConstants;
import com.quincy.sdk.EmailService;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeSender;
import com.quincy.sdk.VCodeOpsRgistry;
import com.quincy.sdk.helper.CommonHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class VCodeOperation implements VCodeOpsRgistry {
	@Value("${auth.vcode.timeout:180}")
	private int vcodeTimeoutSeconds;
	private static final Map<String, String> I18N_KEYS_HOLDER = new HashMap<String, String>(2);
	static {
		I18N_KEYS_HOLDER.put(VCodeConstants.ATTR_KEY_VCODE_ROBOT_FORBIDDEN, "vcode.name.vcode");
		I18N_KEYS_HOLDER.put(VCodeConstants.ATTR_KEY_VCODE_LOGIN, "vcode.name.password");
		I18N_KEYS_HOLDER.put(VCodeConstants.ATTR_KEY_VCODE_SIGNUP, "vcode.name.vcode");
	}

	public char[] generate(VCodeCharsFrom _charsFrom, int length) {
		String charsFrom = (_charsFrom==null?VCodeCharsFrom.MIXED:_charsFrom).getValue();
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		char[] _vcode = new char[length];
		for(int i=0;i<length;i++) {
			char c = charsFrom.charAt(random.nextInt(charsFrom.length()));
			sb.append(c);
			_vcode[i] = c;
		}
		return _vcode;
	}

	public Result validate(HttpServletRequest request, boolean ignoreCase, String attrKey) throws Exception {
		return this.validate(request, ignoreCase, attrKey, attrKey);
	}

	public Result validate(HttpServletRequest request, boolean ignoreCase, String attrKey, String msgKey) throws Exception {
		HttpSession session = request.getSession(false);
		String inputedVCode = CommonHelper.trim(request.getParameter(VCodeConstants.PARA_NAME_VCODE));
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
				Object _cachedVCode = session.getAttribute(attrKey);
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
			session.removeAttribute(attrKey);
			status = 1;
		} else {
			RequestContext requestContext = new RequestContext(request);
			msg = requestContext.getMessage(msgI18NKey, new Object[] {requestContext.getMessage(I18N_KEYS_HOLDER.get(msgKey))});
		}
		return new Result(status, msg);
	}
	/**
	 * 用于临时密码登录，临时密码发送方式可以通过VCodeSender定制，通常是发邮件、短信、IM软件推送
	 */
	public String genAndSend(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCodeSender sender) throws Exception {
		char[] vcode = this.generate(charsFrom, length);
		HttpSession session = request.getSession();
		session.setAttribute(VCodeConstants.ATTR_KEY_VCODE_LOGIN, new String(vcode));
		session.setAttribute(VCodeConstants.ATTR_KEY_VCODE_ORIGINAL_MAX_INACTIVE_INTERVAL, session.getMaxInactiveInterval());
		session.setMaxInactiveInterval(vcodeTimeoutSeconds);
		sender.send(vcode, vcodeTimeoutSeconds/60);
		return session.getId();
	}

	@Autowired
	private EmailService emailService;
	/**
	 * 通过发邮件传递临时密码
	 */
	public String genAndSend(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String emailTo, String subject, String _content) throws Exception {
		return this.genAndSend(request, charsFrom, length, new VCodeSender() {
			@Override
			public void send(char[] _vcode, int expireMinuts) {
				String vcode = new String(_vcode);
				String content = MessageFormat.format(_content, vcode, expireMinuts);
				emailService.send(emailTo, subject, content);
			}
		});
	}
}