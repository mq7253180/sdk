package com.quincy.core.redis;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.quincy.sdk.RedisProcessor;

@Controller
@RequestMapping("")
public class VCodeController {
	@Autowired
	private RedisProcessor redisProcessor;

	@RequestMapping("/auth/vcode/{width}/{height}")
	public void genVCode(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable(required = true, name = "width")int width, 
			@PathVariable(required = true, name = "height")int height) throws IOException {
		redisProcessor.vcode(request, response, width, height);
	}
}