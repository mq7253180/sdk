package com.quincy.auth.freemarker;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.quincy.sdk.AuthHelper;
import com.quincy.sdk.o.XSession;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public abstract class AbstractHtmlTemplateDirectiveModel implements TemplateDirectiveModel {
	protected abstract String reallyExecute(Environment env, Map params, TemplateModel[] loopVars) throws IOException;

	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
			throws TemplateException, IOException {
		boolean output = false;
		Object permission = params.get("permission");
		if(permission==null) {
			output = true;
		} else {
			String permissionName = permission.toString();
			XSession session = AuthHelper.getSession();
			List<String> permissions = session.getPermissions();
			for(String p:permissions) {
				if(p.equalsIgnoreCase(permissionName)) {
					output = true;
					break;
				}
			}
		}
		if(output) {
			String html = this.reallyExecute(env, params, loopVars);
			if(html!=null) {
				if(body!=null) {
					body.render(new PlaceHolderWriter(env.getOut(), html));
				} else
					env.getOut().write(html);
			}
		}
	}

	private static class PlaceHolderWriter extends Writer {
		private final Writer out;
		private String html;
		
		public PlaceHolderWriter(Writer out, String html) {
            this.out = out;
            this.html = html;
        }

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			int index = html.indexOf("</");
			StringBuilder sb = new StringBuilder(200).append(html.substring(0, index)).append(cbuf).append(html.substring(index, html.length()));
			out.write(sb.toString());
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
		}
	}
}