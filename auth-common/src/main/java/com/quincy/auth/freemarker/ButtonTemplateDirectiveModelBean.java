package com.quincy.auth.freemarker;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateModel;

public class ButtonTemplateDirectiveModelBean extends AbstractHtmlTemplateDirectiveModel {
	@Override
	protected String reallyExecute(Environment env, Map params, TemplateModel[] loopVars) throws IOException {
		Object id = params.get("id");
		Object name = params.get("name");
		Object clazz = params.get("class");
		Object dataToggle = params.get("dataToggle");
		Object dataTarget = params.get("dataTarget");
		StringBuilder html = new StringBuilder(200).append("<button");
		if(id!=null)
			html.append(" id=\"").append(id.toString()).append("\"");
		if(name!=null)
			html.append(" name=\"").append(name.toString()).append("\"");
		if(clazz!=null)
			html.append(" class=\"").append(clazz.toString()).append("\"");
		if(dataToggle!=null)
			html.append(" data-toggle=\"").append(dataToggle.toString()).append("\"");
		if(dataTarget!=null)
			html.append(" data-target=\"").append(dataTarget.toString()).append("\"");
		return html.append("></button>").toString();
	}
}