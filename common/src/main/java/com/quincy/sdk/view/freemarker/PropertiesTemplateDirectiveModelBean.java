package com.quincy.sdk.view.freemarker;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class PropertiesTemplateDirectiveModelBean implements TemplateDirectiveModel {
	private Properties properties;

	public PropertiesTemplateDirectiveModelBean(Properties properties) {
		this.properties = properties;
	}

	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
			throws TemplateException, IOException {
		Object key = params.get("key");
		Writer out = env.getOut();
		String value = properties.getProperty(key.toString());
		out.write(value==null?"["+key.toString()+": null]":value);
	}

	public Properties getProperties() {
		return properties;
	}
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
