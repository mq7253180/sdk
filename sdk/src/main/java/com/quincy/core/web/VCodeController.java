package com.quincy.core.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.quincy.core.VCodeConstants;
import com.quincy.core.InnerHelper;
import com.quincy.sdk.Result;
import com.quincy.sdk.VCodeCharsFrom;
import com.quincy.sdk.VCodeOpsRgistry;
import com.quincy.sdk.annotation.VCodeRequired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/vcode")
public class VCodeController extends HandlerInterceptorAdapter {
	private final double VCODE_RADIANS = Math.PI/180;
	@Value("${vcode.length}")
	private int vcodeLength;
	@Value("${vcode.lines}")
	private int vcodeLines;
	@Value("${auth.center:}")
	private String authCenter;
	@Autowired
	private VCodeOpsRgistry vCodeOpsRgistry;
	/**
	 * Example: 25/10/25/110/35
	 */
	@RequestMapping("/{size}/{start}/{space}/{width}/{height}")
	public void genVCode(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable(required = true, name = "size")int size,
			@PathVariable(required = true, name = "start")int start,
			@PathVariable(required = true, name = "space")int space,
			@PathVariable(required = true, name = "width")int width, 
			@PathVariable(required = true, name = "height")int height) throws Exception {
		char[] vcode = vCodeOpsRgistry.generate(VCodeCharsFrom.MIXED, vcodeLength);
		request.getSession().setAttribute(VCodeConstants.ATTR_KEY_VCODE_ROBOT_FORBIDDEN, new String(vcode));
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
		for(char c:vcode) {
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

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod) {
			HandlerMethod method = (HandlerMethod)handler;
			VCodeRequired annotation = method.getMethod().getDeclaredAnnotation(VCodeRequired.class);
			if(annotation!=null) {
				Result result = vCodeOpsRgistry.validate(request, annotation.ignoreCase(), VCodeConstants.ATTR_KEY_VCODE_ROBOT_FORBIDDEN);
				if(result.getStatus()<1) {
					InnerHelper.outputOrForward(request, response, handler, result, authCenter+"/failure", false);
					return false;
				}
			}
		}
		return true;
	}
}