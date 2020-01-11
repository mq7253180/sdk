package com.quincy.core.sftp;

import com.jcraft.jsch.ChannelSftp;

public interface ChannelSftpSource {
	public ChannelSftp get() throws Exception;
	public void close();
}
