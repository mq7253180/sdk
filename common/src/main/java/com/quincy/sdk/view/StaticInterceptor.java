package com.quincy.sdk.view;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class StaticInterceptor extends HandlerInterceptorAdapter {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(request.getRequestURI().endsWith(".properties"))
			response.setContentType("application/octet-stream");
		String uri = request.getRequestURI();
		String location = this.getClass().getResource("/").getPath().replaceAll("target/classes/", "src/main")+uri;
		InputStream in = null;
		OutputStream out = null;
		Writer writer = null;
		try {
			in = new BufferedInputStream(new FileInputStream(location));
			byte[] buf = new byte[in.available()];
			in.read(buf);
			out = response.getOutputStream();
			out.write(buf);
			/*writer = response.getWriter();
			writer.write(location);*/
		} finally {
			if(in!=null)
				in.close();
			if(out!=null)
				out.close();
			if(writer!=null)
				writer.close();
		}
		return false;
	}
}
