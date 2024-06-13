package com.quincy.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.core.AuthCommonConstants;
import com.quincy.core.InnerConstants;
import com.quincy.core.VCodeStore;
import com.quincy.sdk.VCodeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthorizationCommonController {
	/**
	 * 登出
	 */
	@RequestMapping("/signout")
	public String logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		if(session!=null) {
			session.invalidate();
		}
		return InnerConstants.VIEW_PATH_RESULT;
	}
	/**
	 * 点超链接没权限要进入的页面
	 */
	@RequestMapping("/deny")
	public String deny() {
		return "/deny";
	}

	@Autowired
	private VCodeService vCodeService;
	/**
	 * Example: 25/10/25/110/35
	 * 用于密码登录时输错超过一定次数
	 */
	@RequestMapping("/vcode/{size}/{start}/{space}/{width}/{height}")
	public void genVCode(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable(required = true, name = "size")int size,
			@PathVariable(required = true, name = "start")int start,
			@PathVariable(required = true, name = "space")int space,
			@PathVariable(required = true, name = "width")int width, 
			@PathVariable(required = true, name = "height")int height) throws Exception {
		vCodeService.outputVcode(new VCodeStore() {
			@Override
			public void save(char[] vcode) {
				vCodeService.saveVcode(request.getSession(), vcode, AuthCommonConstants.ATTR_KEY_VCODE_PWD_LOGIN);
			}
		}, response, size, start, space, width, height);
	}
}