package com.quincy.auth;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.helper.CommonHelper;

public class Oauth2Helper {
	public static String serverErrorUri(int errorStatus, String locale) {
		return serverErrorUri(errorStatus, locale, "/oauth2/error?status=");
	}

	public static String serverErrorUri(int errorStatus, String locale, String uriPrefix) {
		return CommonHelper.appendUriParam(new StringBuilder(100).append(uriPrefix).append(errorStatus), InnerConstants.KEY_LOCALE, locale).toString();
	}
}