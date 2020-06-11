package com.quincy.auth.freemarker;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateModel;

public class InputTemplateDirectiveModelBean extends AbstractHtmlTemplateDirectiveModel {
	@Override
	protected String reallyExecute(Environment env, Map params, TemplateModel[] loopVars) throws IOException {
		Object type = params.get("type");
		Object id = params.get("id");
		Object name = params.get("name");
		Object clazz = params.get("class");
		Object value = params.get("value");
		StringBuilder html = new StringBuilder(200).append("<input");
		if(type!=null)
			html.append(" type=\"").append(type.toString()).append("\"");
		if(id!=null)
			html.append(" id=\"").append(id.toString()).append("\"");
		if(name!=null)
			html.append(" name=\"").append(name.toString()).append("\"");
		if(clazz!=null)
			html.append(" class=\"").append(clazz.toString()).append("\"");
		if(value!=null)
			html.append(" value=\"").append(value.toString()).append("\"");
		return html.append(" />").toString();
	}
}