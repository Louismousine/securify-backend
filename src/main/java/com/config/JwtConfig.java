package com.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.services.JwtTokenProvider;
//class that applies JWT to the application, and allows all routes to be accessed
@Configuration
public class JwtConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	JwtTokenProvider jwtTokenProvider;

	//dont know what this does
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	//allow tokens
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.apply(new JwtConfigurer(jwtTokenProvider));
	}
	//allow all users to access register and login pages
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/**");
		
	}
}
