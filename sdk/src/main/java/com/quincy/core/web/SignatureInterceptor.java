package com.quincy.core.web;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.annotation.SignatureRequired;
import com.quincy.sdk.helper.CommonHelper;
import com.quincy.sdk.helper.HttpClientHelper;
import com.quincy.sdk.helper.RSASecurityHelper;

public class SignatureInterceptor extends HandlerInterceptorAdapter {
	private final static String mapKey = "signature";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			SignatureRequired annotation = method.getMethod().getDeclaredAnnotation(SignatureRequired.class);
			if(annotation!=null) {
				Integer status = null;
				String msgI18NKey = null;
				String signature = CommonHelper.trim(request.getParameter(mapKey));
				if(signature==null) {
					status = -6;
					msgI18NKey = "signature.null";
				} else {
					Map<String, String[]> map = request.getParameterMap();
					map.remove(mapKey);
					Iterator<Entry<String, String[]>> it = map.entrySet().iterator();
					StringBuilder sb = new StringBuilder(200);
					while(it.hasNext()) {
						Entry<String, String[]> e = it.next();
						sb.append("&").append(e.getKey()).append("=").append(e.getValue()[0]);
					}
					String publicKey = "";
					if(!RSASecurityHelper.verify(publicKey, RSASecurityHelper.SIGNATURE_ALGORITHMS_SHA1_RSA, signature, sb.substring(1, sb.length()), null)) {
						status = -7;
						msgI18NKey = "signature.not_matched";
					}
				}
				if(status==null) {
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