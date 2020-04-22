package com.quincy.auth;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

public class Oauth2Helper {
	public static String serverErrorUri(int errorStatus, String locale) {
		return errorUri(errorStatus, locale, "/oauth2/error/server?status=");
	}

	public static String resourceErrorUri(int errorStatus, String locale) {
		return errorUri(errorStatus, locale, "/oauth2/error/resource?status=");
	}

	private static String errorUri(int errorStatus, String locale, String uriPrefix) {
		return CommonHelper.appendUriParam(new StringBuilder(100).append(uriPrefix).append(errorStatus), InnerConstants.KEY_LOCALE, locale).toString();
	}
}