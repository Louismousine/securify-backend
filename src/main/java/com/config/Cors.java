package com.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//this config class allows all types of requests to be made to the back-end
//without it, delete, put, patch, etc are denied access
@Configuration
public class Cors implements WebMvcConfigurer{
	//the frontend URL
	@Value("${corspolicyurl}")
	private String url;
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("*").allowedOrigins(url);
	}
}
