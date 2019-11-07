package com.quincy.auth.freemarker;

import java.io.IOException;
import java.util.Map;

import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateModel;

public class ButtonTemplateDirectiveModelBean extends AbstractHtmlTemplateDirectiveModel {
	@Override
	protected String realExecute(Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws IOException {
		Object id = params.get("id");
		Object name = params.get("name");
		Object clazz = params.get("class");
		Object dataToggle = params.get("dataToggle");
		Object dataTarget = params.get("dataTarget");
		StringBuilder html = new StringBuilder(200);
		html.append("<button");
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
		if(dataToggle!=null) {
			html.append(" data-toggle=\"");
			html.append(dataToggle.toString());
			html.append("\"");
		}
		if(dataTarget!=null) {
			html.append(" data-target=\"");
			html.append(dataTarget.toString());
			html.append("\"");
		}
		html.append("></button>");
		return html.toString();
	}
}
