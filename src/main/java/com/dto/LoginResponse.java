package com.dto;

import lombok.Data;

@Data
public class LoginResponse {

	private String email;
	private String username;
	private boolean isAdmin;
	private String token;
}
