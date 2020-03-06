package com.quincy.core.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.quincy.sdk.entity.Region;
import com.quincy.sdk.service.RegionService;

@Controller
@RequestMapping(value = "/region")
public class RegionController {
	@Autowired
	private RegionService regionService;
	@Value("${env}")
	private String env;

	@GetMapping(value = "/all")
	public ModelAndView list() {
		/*log.info("this.getClass().getResource--------"+this.getClass().getResource("").getPath());
		log.info("this.getClass().getResource/--------"+this.getClass().getResource("/").getPath());
		log.info("Thread.currentThread().getContextClassLoader().getResource--------"+Thread.currentThread().getContextClassLoader().getResource("").getPath());
		log.info("this.getClass().getClassLoader().getResource--------"+this.getClass().getClassLoader().getResource("").getPath());
		if(Constants.ENV_DEV.equals(env))
			log.info("ClassLoader.getSystemResource--------"+ClassLoader.getSystemResource("").getPath());*/
		return new ModelAndView("/content/region").addObject("regions", regionService.findAll());
	}

	@GetMapping(value = "/all2")
	@ResponseBody
	public List<Region> countries2() {
		return regionService.findAll();
	}

	@GetMapping(value = "/all3")
	public String countries3() {
		return "redirect:/region/all2";
	}
}
