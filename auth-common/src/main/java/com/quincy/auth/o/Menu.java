package com.quincy.auth.o;

import java.util.List;

public interface Menu {
	public Long getId();
	public Long getPId();
	public String getName();
	public String getUri();
	public String getIcon();
	public <T extends Menu> List<T> getChildren();
}