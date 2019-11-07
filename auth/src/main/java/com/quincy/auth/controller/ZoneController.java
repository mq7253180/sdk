package com.quincy.auth.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.auth.entity.Country;
import com.quincy.auth.service.ZoneService;

@Controller
@RequestMapping(value = "/zone")
public class ZoneController {
	@Autowired
	private ZoneService zoneService;
	@Value("${env}")
	private String env;

	@GetMapping(value = "/countries")
	public ModelAndView countries() {
		/*log.info("this.getClass().getResource--------"+this.getClass().getResource("").getPath());
		log.info("this.getClass().getResource/--------"+this.getClass().getResource("/").getPath());
		log.info("Thread.currentThread().getContextClassLoader().getResource--------"+Thread.currentThread().getContextClassLoader().getResource("").getPath());
		log.info("this.getClass().getClassLoader().getResource--------"+this.getClass().getClassLoader().getResource("").getPath());
		if(Constants.ENV_DEV.equals(env))
			log.info("ClassLoader.getSystemResource--------"+ClassLoader.getSystemResource("").getPath());*/
		ModelAndView mv = new ModelAndView("/content/country");
		mv.addObject("countries", zoneService.findCountries());
		return mv;
	}

	@GetMapping(value = "/countries2")
	@ResponseBody
	public List<Country> countries2() {
		return zoneService.findCountries();
	}

	@GetMapping(value = "/countries3")
	public String countries3() {
		return "redirect:/zone/countries2";
	}
}
