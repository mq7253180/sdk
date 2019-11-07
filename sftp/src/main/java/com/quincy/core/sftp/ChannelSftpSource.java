package com.quincy.core.sftp;

import java.util.NoSuchElementException;

import com.jcraft.jsch.ChannelSftp;

public interface ChannelSftpSource {
	public ChannelSftp get() throws NoSuchElementException, IllegalStateException, Exception;
	public void close();
}
