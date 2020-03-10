package com.quincy.sdk.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quincy.sdk.dao.RegionRepository;
import com.quincy.sdk.entity.Region;
import com.quincy.sdk.service.RegionService;

@Service
public class RegionServiceImpl implements RegionService {
	@Autowired
	private RegionRepository regionRepository;

	@Override
	public List<Region> findAll() {
		return regionRepository.findAll();
	}

	@Override
	public List<Region> findCountries() {
		return regionRepository.findByParentId(0l);
	}
}
