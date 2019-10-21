package com.web.console.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>简要说明...</p>
 *
 * @author JingHe
 * @version 1.0
 * @date 2019/9/16
 */
@RestController
public class TestController {

	private Logger logger = LoggerFactory.getLogger(TestController.class);

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private HttpServletResponse response;



	@RequestMapping("/test")
	public String test() {
		Map<String, String[]> paramMap = request.getParameterMap();
		paramMap.forEach((key, value) -> {
			logger.info("request param key:{}, value:{}", key, value);
		});
		return "hello SpringBoot!";
	}
}
