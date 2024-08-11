package com.quincy.core;

import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;

public class InnerConstants {
	public final static String KEY_LOCALE = "locale";
//	public final static String BEAN_NAME_PROPERTIES = "quincyPropertiesFactory";
	public final static String PARAM_REDIRECT_TO = "redirectTo";
	public final static String VIEW_PATH_SUCCESS = "/success";
	public final static String VIEW_PATH_FAILURE = "/failure";
//	public final static KeyFingerPrintCalculator KEY_FINGER_PRINT_CALCULATOR = new JcaKeyFingerprintCalculator();
	public final static KeyFingerPrintCalculator KEY_FINGER_PRINT_CALCULATOR = new BcKeyFingerprintCalculator();
	public final static String BEAN_NAME_PROPERTIES = "mailProperties";
	public final static String BEAN_NAME_SYS_THREAD_POOL = "sysThreadPoolExecutor";
}