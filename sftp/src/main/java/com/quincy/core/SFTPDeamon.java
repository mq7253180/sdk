package com.quincy.core;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.quincy.core.sftp.ChannelSftpSource;

@Component
public class SFTPDeamon {
	@Autowired
	private ChannelSftpSource channelSftpSource;

	@PreDestroy
	public void destroy() {
		if(channelSftpSource!=null)
			channelSftpSource.close();
	}
}