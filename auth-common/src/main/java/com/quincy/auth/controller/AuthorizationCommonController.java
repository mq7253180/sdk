package com.quincy.auth.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Random;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.auth.AuthCommonConstants;
import com.quincy.core.InnerConstants;
import com.quincy.sdk.EmailService;

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

	private final double VCODE_RADIANS = Math.PI/180;
	@Value("${auth.vcode.length}")
	private int vcodeLength;
	@Value("${auth.vcode.lines}")
	private int vcodeLines;
	@Value("${auth.vcode.timeout:120}")
	private int vcodeTimeoutSeconds;

	private char[] genVcode(VCodeCharsFrom _charsFrom, int length) {
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
	/**
	 * 用于临时密码登录，临时密码发送方式可以通过VCodeSender定制，通常是发邮件、短信、IM软件推送
	 */
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, VCodeSender sender) throws Exception {
		char[] _vcode = this.genVcode(charsFrom, length);
		HttpSession session = request.getSession();
		session.setAttribute(AuthCommonConstants.VCODE_ATTR_KEY, new String(_vcode));
		session.setAttribute(AuthCommonConstants.VCODE_ORIGINAL_MXA_INACTIVE_INTERVAL_ATTR_KEY, session.getMaxInactiveInterval());
		session.setMaxInactiveInterval(vcodeTimeoutSeconds);
		sender.send(_vcode);
		return session.getId();
	}

	@Autowired
	private EmailService emailService;
	/**
	 * 通过发邮件传递临时密码
	 */
	public String vcode(HttpServletRequest request, VCodeCharsFrom charsFrom, int length, String emailTo, String subject, String _content) throws Exception {
		return this.vcode(request, charsFrom, length, new VCodeSender() {
			@Override
			public void send(char[] _vcode) {
				String vcode = new String(_vcode);
				String content = MessageFormat.format(_content, vcode);
				emailService.send(emailTo, subject, content, "", null, null, null, null);
			}
		});
	}
	/**
	 * Example: 25/10/25/110/35
	 */
	@RequestMapping("/vcode/{size}/{start}/{space}/{width}/{height}")
	public void genVCode(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable(required = true, name = "size")int size,
			@PathVariable(required = true, name = "start")int start,
			@PathVariable(required = true, name = "space")int space,
			@PathVariable(required = true, name = "width")int width, 
			@PathVariable(required = true, name = "height")int height) throws Exception {
		char[] _vcode = this.genVcode(VCodeCharsFrom.MIXED, height);
		String vcode = new String(_vcode);
		/*
		 * 存redis
		 */
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		Graphics g = image.getGraphics();
		Graphics2D gg = (Graphics2D)g;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);//填充背景
		Random random = new Random();
		for(int i=0;i<vcodeLines;i++) {
			g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
			g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
		}
		Font font = new Font("Times New Roman", Font.ROMAN_BASELINE, size);
		g.setFont(font);
//		g.translate(random.nextInt(3), random.nextInt(3));
        int x = start;//旋转原点的 x 坐标
		for(char c:_vcode) {
            double tiltAngle = random.nextInt()%30*VCODE_RADIANS;//角度小于30度
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
}