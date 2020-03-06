package com.quincy.core.web.freemarker;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import org.springframework.web.servlet.support.RequestContext;

import com.quincy.sdk.helper.CommonHelper;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class I18NTemplateDirectiveModelBean implements TemplateDirectiveModel {
	private Properties properties;

	public I18NTemplateDirectiveModelBean(Properties properties) {
		this.properties = properties;
	}

	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
			throws TemplateException, IOException {
		RequestContext requestContext = new RequestContext(CommonHelper.getRequest());
		String msg = requestContext.getMessage(params.get("key").toString());
		if(body!=null) {
			body.render(new PlaceHolderWriter(env.getOut(), requestContext, properties, msg));
		} else
			env.getOut().write(msg);
	}

	private static class PlaceHolderWriter extends Writer {
		private RequestContext requestContext;
		private Properties properties;
		private final Writer out;
		private String msg;
		
		public PlaceHolderWriter(Writer out, RequestContext requestContext, Properties properties, String msg) {
            this.out = out;
            this.msg = msg;
            this.requestContext = requestContext;
            this.properties = properties;
        }

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			String[] sbuf = new String(cbuf).split(",");
			Object[] args = new Object[sbuf.length];
			for(int i=0;i<sbuf.length;i++) {
				String s = sbuf[i];
				String[] placeholderValue = s.split(":");
				String type = null;
				String value = null;
				if(placeholderValue.length==1) {
					args[i] = placeholderValue[0];
				} else if(placeholderValue.length>1) {
					type = placeholderValue[0].trim();
					value = placeholderValue.length>1?placeholderValue[1].trim():type;
					if("property".equals(type)) {
						args[i] = properties.getProperty(value);
					} else if("i18n".equals(type))
						args[i] = requestContext.getMessage(value);
				}
			}
			out.write(MessageFormat.format(msg, args));
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

	public Properties getProperties() {
		return properties;
	}
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}