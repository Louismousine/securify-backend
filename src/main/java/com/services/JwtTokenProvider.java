package com.services;

import java.util.Base64;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.entities.UserEntity;
import com.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.*;

//this class generates tokens
@Component
public class JwtTokenProvider {
	@Value("${security.jwt.token.secret-key:secret}")
	private String secretKey = "secret";
	//a token is valid for 1h
	@Value("${token.validity.time}")
	private long validityInMilliseconds; // 1h

	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private MessageSource ms;

	@PostConstruct
	protected void init() {
		secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
	}

	// creates a token
	public String createToken(String username) {
		Claims claims = Jwts.claims().setSubject(username);
		Date now = new Date();
		Date validity = new Date(now.getTime() + validityInMilliseconds);
		return Jwts.builder()//
				.setClaims(claims)//
				.setIssuedAt(now)//
				.setExpiration(validity)//
				.signWith(SignatureAlgorithm.HS256, secretKey)//
				.compact();
	}

	// dont know what this does but it looks important
	public Authentication getAuthentication(String token) {
		UserEntity userDetails = this.userRepo.findByUsername(getUsername(token));
		return new UsernamePasswordAuthenticationToken(userDetails, "", null);
	}

	//extracts the username from the token
	public String getUsername(String token) {
		return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
	}

	// dont know what this does but it looks important
	public String resolveToken(HttpServletRequest req) {
		String bearerToken = req.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7, bearerToken.length());
		}
		return null;
	}

	//check that the validation was made before the expiry time
	public boolean validateToken(String token) {
		try {
			Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
			if (claims.getBody().getExpiration().before(new Date())) {
				return true;
			}
			return false;
		} catch (JwtException | IllegalArgumentException e) {
			throw new RuntimeException(ms.getMessage("invalidToken", null, LocaleContextHolder.getLocale()));
		}
	}
}
