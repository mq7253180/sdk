package com.quincy.core;

import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;

public class InnerConstants {
	public final static int VCODE_LENGTH = 4;//验证码长度
	public final static String MV_ATTR_NAME_STATUS = "status";
	public final static String MV_ATTR_NAME_MSG = "msg";
	public final static String KEY_LOCALE = "locale";
	public final static String CLIENT_TYPE_P = "p";
	public final static String CLIENT_TYPE_M = "m";
	public final static String CLIENT_TYPE_J = "j";
	public final static String CLIENT_APP = "app_client";
	public final static String CLIENT_TOKEN_PROPERTY_NAME = "clientTokenName";
	public final static String ATTR_SESSION = "dddsession";//改了会影响页面模板，要同时改
	public final static String ATTR_VCODE = "vcode";
	public final static String BEAN_NAME_PROPERTIES = "quincyPropertiesFactory";
	public final static String PARAM_REDIRECT_TO = "redirectTo";
	public final static String VIEW_PATH_RESULT = "/result";
//	public final static KeyFingerPrintCalculator KEY_FINGER_PRINT_CALCULATOR = new JcaKeyFingerprintCalculator();
	public final static KeyFingerPrintCalculator KEY_FINGER_PRINT_CALCULATOR = new BcKeyFingerprintCalculator();
}