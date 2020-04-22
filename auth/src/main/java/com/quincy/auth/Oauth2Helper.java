package com.quincy.auth;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

public class Oauth2Helper {
	private final static String ERROR_URI = "/oauth2/error?status=";

	public static String errorUri(int errorStatus, String locale) {
		return CommonHelper.appendUriParam(new StringBuilder(100).append(Oauth2Helper.ERROR_URI).append(errorStatus), InnerConstants.KEY_LOCALE, locale).toString();
	}
}