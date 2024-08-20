package com.quincy.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;

import com.quincy.auth.o.Enterprise;
import com.quincy.auth.o.XSession;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.annotation.CustomizedInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@CustomizedInterceptor(pathPatterns = "/**")
public class MultiEnterpriseConfiguration extends HandlerInterceptorAdapter {
	@Value("${auth.center:}")
	private String authCenter;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		XSession xsession = AuthHelper.getSession(request);
		if(xsession.getUser().getCurrentEnterprise()==null) {
			List<Enterprise> enterprises = xsession.getUser().getEnterprises();
			/*enterprises = new ArrayList<>();
			Enterprise e = new Enterprise();
			e.setId(1l);
			e.setName("TIBCO");
			e.setShardingKey(1234);
			enterprises.add(e);
			e = new Enterprise();
			e.setId(2l);
			e.setName("QAM");
			e.setShardingKey(2345);
			enterprises.add(e);
			e = new Enterprise();
			e.setId(1l);
			e.setName("IZP");
			e.setShardingKey(3456);
			enterprises.add(e);*/
			InnerHelper.outputOrRedirect(request, response, handler, -8, new RequestContext(request).getMessage("auth.noenterprise"), enterprises, authCenter+AuthConstants.URI_TO_ENTERPRISE_SELECTION, true);
			return false;
		} else
			return true;
	}

	public String toEnterpriseSelection() {
		return "/enterprise_selection";
	}
}