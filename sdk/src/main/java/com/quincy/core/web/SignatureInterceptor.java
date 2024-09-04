package com.quincy.core.web;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.core.PublicKeyGetter;
import com.quincy.sdk.annotation.SignatureRequired;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SignatureInterceptor extends HandlerInterceptorAdapter {
	private PublicKeyGetter exchanger;

	public SignatureInterceptor(PublicKeyGetter exchanger) {
		this.exchanger = exchanger;
	}

	private final static String MAP_KEY = "signature";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			SignatureRequired annotation = method.getMethod().getDeclaredAnnotation(SignatureRequired.class);
			if(annotation!=null) {
				Integer status = null;
				String msgI18NKey = null;
				String id = CommonHelper.trim(request.getParameter("id"));
				String signature = CommonHelper.trim(request.getParameter(MAP_KEY));
				if(id==null||signature==null) {
					status = -2;
					msgI18NKey = "signature.null";
				} else {
					String publicKey = CommonHelper.trim(exchanger.getById(id));
					if(publicKey==null) {
						throw new RuntimeException("Public key is null.");
					} else {
						Map<String, String[]> map = request.getParameterMap();
						Iterator<Entry<String, String[]>> it = map.entrySet().iterator();
						StringBuilder sb = new StringBuilder(200);
						while(it.hasNext()) {
							Entry<String, String[]> e = it.next();
							if(e.getValue()!=null&&e.getValue().length>0&&!MAP_KEY.equals(e.getKey()))
								sb.append("&").append(e.getKey()).append("=").append(e.getValue()[0]);
						}
						if(!RSASecurityHelper.verify(publicKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, signature, sb.substring(1, sb.length()), null)) {
							status = -3;
							msgI18NKey = "signature.not_matched";
						}
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
}