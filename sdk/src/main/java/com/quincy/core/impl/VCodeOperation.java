package com.quincy.core.impl;

import java.text.MessageFormat;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.core.AuthCommonConstants;
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
	@Value("${auth.vcode.timeout:120}")
	private int vcodeTimeoutSeconds;

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

	public Result validate(HttpServletRequest request, boolean ignoreCase, String attrKey, String nameI18NKey) throws Exception {
		HttpSession session = request.getSession(false);
		String inputedVCode = CommonHelper.trim(request.getParameter(AuthCommonConstants.PARA_NAME_VCODE));
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
			msg = requestContext.getMessage(msgI18NKey, new Object[] {requestContext.getMessage(nameI18NKey)});
		}
		return new Result(status, msg);
	}
	/**
	 * 用于临时密码登录，临时密码发送方式可以通过VCodeSender定制，通常是发邮件、短信、IM软件推送
	 */
	public String genAndSend(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCodeSender sender) throws Exception {
		char[] vcode = this.generate(charsFrom, length);
		HttpSession session = request.getSession();
		session.setAttribute(AuthCommonConstants.ATTR_KEY_USERNAME, request.getParameter(AuthCommonConstants.PARA_NAME_USERNAME));
		session.setAttribute(AuthCommonConstants.ATTR_KEY_VCODE_LOGIN, new String(vcode));
		session.setAttribute(AuthCommonConstants.ATTR_KEY_VCODE_ORIGINAL_MXA_INACTIVE_INTERVAL, session.getMaxInactiveInterval());
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
				emailService.send(emailTo, subject, content, "", null, null, null, null);
			}
		});
	}
}