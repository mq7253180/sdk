package com.quincy.sdk.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quincy.sdk.dao.RegionDao;
import com.quincy.sdk.entity.Region;
import com.quincy.sdk.service.RegionService;

@Service
public class RegionServiceImpl implements RegionService {
	@Autowired
	private RegionDao regionDao;

	@Override
	public List<Region> findAll() {
		return regionDao.find();
	}

	@Override
	public List<Region> findCountries() {
		return regionDao.findByParentId(0l);
	}
}
