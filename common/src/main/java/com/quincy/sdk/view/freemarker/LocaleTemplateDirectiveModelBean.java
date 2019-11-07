package com.quincy.sdk.view.freemarker;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import com.quincy.core.Sync;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class LocaleTemplateDirectiveModelBean implements TemplateDirectiveModel {
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
			throws TemplateException, IOException {
		Locale locale = Sync.getLocaleThreadLocal().get();
		env.getOut().write(locale.getLanguage()+"_"+locale.getCountry());
	}
}
