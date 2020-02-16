package com.quincy.sdk;

import lombok.Data;

@Data
public class PoolParams {
	private Integer maxTotal;
	private Integer maxIdle;
	private Integer minIdle;
	private Long maxWaitMillis;
	private Long minEvictableIdleTimeMillis;
	private Long timeBetweenEvictionRunsMillis;
	private Integer numTestsPerEvictionRun;
//	private Boolean blockWhenExhausted;
	private Boolean testOnBorrow;
	private Boolean testWhileIdle;
	private Boolean testOnReturn;
//	private Boolean removeAbandonedOnMaintenance;
//	private Boolean removeAbandonedOnBorrow;
//	private Integer removeAbandonedTimeout;
}