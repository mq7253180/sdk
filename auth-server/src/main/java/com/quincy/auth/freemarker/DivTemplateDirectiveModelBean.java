package com.quincy.auth.freemarker;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateModel;

public class DivTemplateDirectiveModelBean extends AbstractHtmlTemplateDirectiveModel {
	@Override
	protected String reallyExecute(Environment env, Map params, TemplateModel[] loopVars) throws IOException {
		Object id = params.get("id");
		Object name = params.get("name");
		Object clazz = params.get("class");
		StringBuilder html = new StringBuilder(200).append("<div");
		if(id!=null)
			html.append(" id=\"").append(id.toString()).append("\"");
		if(name!=null)
			html.append(" name=\"").append(name.toString()).append("\"");
		if(clazz!=null)
			html.append(" class=\"").append(clazz.toString()).append("\"");
		return html.append("></div>").toString();
	}
}