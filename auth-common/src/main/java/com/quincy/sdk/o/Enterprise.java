package com.quincy.sdk.o;

import java.io.Serializable;

import lombok.Data;

@Data
public class Enterprise implements Serializable {
	private static final long serialVersionUID = 7895373473075023488L;
	private Long id;
	private String name;
	private Integer shardingKey;
	private String unifiedSocialcReditIdentifier;
	private String address;
	private String contactName;
	private String contactPhone;
}