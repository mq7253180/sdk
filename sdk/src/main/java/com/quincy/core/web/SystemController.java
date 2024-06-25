package com.quincy.core.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class SystemController {
	@GetMapping("/static/**")
	public void handleStatic() {}
}