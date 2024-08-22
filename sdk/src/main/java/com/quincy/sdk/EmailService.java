package com.quincy.sdk;

import java.io.File;

public interface EmailService {
	public void send(String to, String subject, String content, String attachment, String fileName, String charset, String ccTo, String bccTo);
	public void send(String to, String subject, String content, File attachment, String fileName, String charset, String ccTo, String bccTo);
	public void send(String to, String subject, String content);
}