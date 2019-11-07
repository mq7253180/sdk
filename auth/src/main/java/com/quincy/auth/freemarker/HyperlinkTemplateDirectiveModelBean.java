package com.quincy.auth.freemarker;

import java.io.IOException;
import java.util.Map;

import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateModel;

public class HyperlinkTemplateDirectiveModelBean extends AbstractHtmlTemplateDirectiveModel {
	@Override
	protected String realExecute(Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws IOException {
		Object id = params.get("id");
		Object name = params.get("name");
		Object clazz = params.get("class");
		Object href = params.get("href");
		Object target = params.get("target");
		Object value = params.get("value");
		StringBuilder html = new StringBuilder(200);
		html.append("<a");
		if(id!=null) {
			html.append(" id=\"");
			html.append(id.toString());
			html.append("\"");
		}
		if(name!=null) {
			html.append(" name=\"");
			html.append(name.toString());
			html.append("\"");
		}
		if(clazz!=null) {
			html.append(" class=\"");
			html.append(clazz.toString());
			html.append("\"");
		}
		if(href!=null) {
			html.append(" href=\"");
			html.append(href.toString());
			html.append("\"");
		}
		if(target!=null) {
			html.append(" target=\"");
			html.append(target.toString());
			html.append("\"");
		}
		if(value!=null) {
			html.append(" value=\"");
			html.append(value.toString());
			html.append("\"");
		}
		html.append("></a>");
		return html.toString();
	}
}
