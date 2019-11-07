package com.quincy.sdk;

import lombok.Data;

@Data
public class Pagination {
	private Integer count;
	private Object data;
	private Integer from;
	private Integer to;
	private Integer pages;
	private Integer page;

	public Pagination(Integer count, Integer page, Integer size) {
		this.count = count;
		this.page = page;
		this.from = size*(page-1)+1;
		this.to = this.from+size-1;
		this.pages = count/size;
		int remainder = count%size;
		if(remainder>0)
			this.pages++;
	}
}
