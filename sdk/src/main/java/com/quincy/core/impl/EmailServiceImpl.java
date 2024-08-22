package com.quincy.core.impl;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quincy.core.InnerConstants;
import com.quincy.sdk.EmailService;
import com.quincy.sdk.helper.CommonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_SYS_THREAD_POOL)
	private ThreadPoolExecutor threadPoolExecutor;

	@Override
	public void send(String to, String subject, String content, String attachment, String fileName, String charset, String ccTo, String bccTo) {
		this.send(to, subject, content, attachment!=null&&attachment.length()>0?new File(attachment):null, fileName, charset, ccTo, bccTo);
	}

	@Override
	public void send(String to, String subject, String content) {
		this.send(to, subject, content, "", null, null, null, null);
	}

	@Value("${mail.username}")
	private String username;
	@Value("${mail.password}")
	private String password;

	@Autowired
	@Qualifier(InnerConstants.BEAN_NAME_PROPERTIES)
	private Properties properties;

	@Override
	public void send(String to, String subject, String content, File attachment, String fileName, String _charset, String ccTo, String bccTo) {
		threadPoolExecutor.execute(new Runnable() {
			@Override
			public void run() {
				String charset = CommonHelper.trim(_charset);
				if(charset==null)
					charset = "UTF-8";
				MimeMultipart mimeMultipart = new MimeMultipart("mixed");
				MimeBodyPart mimeBodyPart = new MimeBodyPart();
				try {
					if(content.indexOf("<html")>=0||content.indexOf("<HTML")>=0)
						mimeBodyPart.setContent(content, "text/html;charset="+charset);
					else
						mimeBodyPart.setText(content, charset);
					mimeMultipart.addBodyPart(mimeBodyPart);
					if(attachment!=null) {
						DataSource ds = new FileDataSource(attachment);
						DataHandler dh = new DataHandler(ds);
						MimeBodyPart attchment = new MimeBodyPart();
						attchment.setDataHandler(dh);
						attchment.setFileName(fileName!=null&&fileName.length()>0?fileName:attachment.getName());
						mimeMultipart.addBodyPart(attchment);
					}
					Session session = Session.getInstance(properties, new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(username, password);
						}
					});
					MimeMessage msg = new MimeMessage(session);
					msg.setFrom(new InternetAddress(username));
					msg.setRecipients(RecipientType.TO, getInternetAddresses(to));
					msg.setSubject(subject, charset);
//					msg.setContent(content, "text/html;charset=utf-8");
					msg.setContent(mimeMultipart);
					InternetAddress[] internetAddresses = getInternetAddresses(ccTo);
					if(internetAddresses!=null&&internetAddresses.length>0)
						msg.setRecipients(RecipientType.CC, internetAddresses);
					internetAddresses = getInternetAddresses(bccTo);
					if(internetAddresses!=null&&internetAddresses.length>0)
						msg.setRecipients(RecipientType.BCC, internetAddresses);
				    Transport.send(msg);
				} catch (Exception e) {
					log.error("EMAIL_ERR: "+to, e);
				}
			}
		});
	}

	private InternetAddress[] getInternetAddresses(String _to) throws AddressException {
		String to = CommonHelper.trim(_to);
		if(to!=null) {
			String[] addrs = to.split(",");
			InternetAddress[] internetAddresses = new InternetAddress[addrs.length];
			for(int i=0;i<addrs.length;i++) {
				String addr = addrs[i];
				internetAddresses[i] = new InternetAddress(addr);
			}
			return internetAddresses;
		} else
			return null;
	}
}