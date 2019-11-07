package com.quincy.auth.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quincy.auth.dao.CountryRepository;
import com.quincy.auth.entity.Country;
import com.quincy.auth.service.ZoneService;

@Service
public class ZoneServiceImpl implements ZoneService {
	@Autowired
	private CountryRepository countryRepository;

	@Override
	public List<Country> findCountries() {
		return countryRepository.findAll();
	}
}
