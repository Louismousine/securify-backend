package com.services;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

//taken from stack overflow, dont know what it does
public class JwtTokenFilter extends GenericFilterBean {
    private JwtTokenProvider jwtTokenProvider;
    public JwtTokenFilter(JwtTokenProvider j) {jwtTokenProvider=j;}
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
        throws IOException, ServletException {
        String token = jwtTokenProvider.resolveToken((HttpServletRequest) req);
        try {
			if (token != null && jwtTokenProvider.validateToken(token)) {
			    Authentication auth = token != null ? jwtTokenProvider.getAuthentication(token) : null;
			    SecurityContextHolder.getContext().setAuthentication(auth);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        filterChain.doFilter(req, res);
    }
}