package com.quincy.sdk;

import java.io.IOException;

public interface VCcodeSender {
	public void send(char[] vcode) throws IOException;
}