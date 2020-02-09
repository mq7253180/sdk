package com.quincy.sdk.service;

import java.util.List;

import com.quincy.sdk.entity.Region;

public interface RegionService {
	public List<Region> findAll();
	public List<Region> findCountries();
}
