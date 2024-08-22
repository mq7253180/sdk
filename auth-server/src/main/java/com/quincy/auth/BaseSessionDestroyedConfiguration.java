package com.quincy.auth;

import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

public abstract class BaseSessionDestroyedConfiguration implements ImportAware, SessionInvalidation {
	private Boolean pcBrowserEvict;
	private Boolean mobileBrowserEvict;
	private Boolean appEvict;
	protected abstract Class<?> annotationClass();

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(annotationClass().getName()));
		this.pcBrowserEvict = attributes.getBoolean("pcBrowser");
		this.mobileBrowserEvict = attributes.getBoolean("mobileBrowser");
		this.appEvict = attributes.getBoolean("app");
	}

	public boolean pcBrowserEvict() {
		return this.pcBrowserEvict;
	}

	public boolean mobileBrowserEvict() {
		return this.mobileBrowserEvict;
	}

	public boolean appEvict() {
		return this.appEvict;
	}
}
