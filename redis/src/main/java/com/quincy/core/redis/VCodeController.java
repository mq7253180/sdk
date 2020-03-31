package com.quincy.core.redis;

import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.RedisProcessor;
import com.quincy.sdk.VCodeCharsFrom;

@Controller
@RequestMapping("")
public class VCodeController {
	@Autowired
	private RedisProcessor redisProcessor;
	@Resource(name = InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	/**
	 * Example: 25/10/25/110/35
	 */
	@RequestMapping("/auth/vcode/{size}/{start}/{space}/{width}/{height}")
	public void genVCode(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable(required = true, name = "size")int size,
			@PathVariable(required = true, name = "start")int start,
			@PathVariable(required = true, name = "space")int space,
			@PathVariable(required = true, name = "width")int width, 
			@PathVariable(required = true, name = "height")int height) throws Exception {
		redisProcessor.vcode(request, VCodeCharsFrom.MIXED, Integer.parseInt(properties.getProperty("vcode.length")), null, response, size, start, space, width, height);
	}
}