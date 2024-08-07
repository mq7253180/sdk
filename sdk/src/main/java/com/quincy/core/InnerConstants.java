package com.quincy.core;

import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;

public class InnerConstants {
	public final static int VCODE_LENGTH = 4;//验证码长度
	public final static String MV_ATTR_NAME_STATUS = "status";
	public final static String MV_ATTR_NAME_MSG = "msg";
	public final static String KEY_LOCALE = "locale";
//	public final static String CLIENT_TYPE_P = "p";
//	public final static String CLIENT_TYPE_M = "m";
//	public final static String CLIENT_TYPE_J = "j";
	public final static String CLIENT_TOKEN_PROPERTY_NAME = "clientTokenName";
	public final static String ATTR_SESSION = "xsession";//改了会影响页面模板，要同时改
//	public final static String BEAN_NAME_PROPERTIES = "quincyPropertiesFactory";
	public final static String PARAM_REDIRECT_TO = "redirectTo";
	public final static String VIEW_PATH_SUCCESS = "/success";
	public final static String VIEW_PATH_FAILURE = "/failure";
//	public final static KeyFingerPrintCalculator KEY_FINGER_PRINT_CALCULATOR = new JcaKeyFingerprintCalculator();
	public final static KeyFingerPrintCalculator KEY_FINGER_PRINT_CALCULATOR = new BcKeyFingerprintCalculator();
	public final static String BEAN_NAME_PROPERTIES = "mailProperties";
	public final static String BEAN_NAME_SYS_THREAD_POOL = "sysThreadPoolExecutor";
	public final static String BEAN_NAME_SYS_JEDIS_SOURCE = "sysJedisSource";
	public final static String DYNAMIC_FIELD_LIST_SETTER_METHOD_KEY = "dynamic_field_list_setter_method";
	public final static String DYNAMIC_FIELD_LIST_GETTER_METHOD_KEY = "dynamic_field_list_getter_method";
}