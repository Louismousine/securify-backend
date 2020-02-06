package com.controller;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.dto.LoginResponse;
import com.dto.PasswordChangeDTO;
import com.dto.RefreshTokenResponse;
import com.dto.UserEmailDTO;
import com.dto.UserLoginDTO;
import com.dto.UserRegisterDTO;
import com.entities.UserEntity;
import com.repositories.UserRepository;
import com.services.JwtTokenProvider;
import com.services.RegisterException;
import com.services.UserService;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/*
 * User controller class - allows for creation of users, login of users, and
 * email validation of users.
 */

@RestController
@Validated
public class UserController {

	@Autowired
	private MessageSource ms;
	@Autowired
	private UserService userService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${baseurl}")
	private String url;

	// register
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody(required = true) UserRegisterDTO user) {

		// check if input is valid
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<UserRegisterDTO>> violations = validator.validate(user);
		for (ConstraintViolation<UserRegisterDTO> violation : violations) {
			return new ResponseEntity<>(violation.getMessage(), HttpStatus.BAD_REQUEST);
		}

		try {
			userService.addUser(user);
			return new ResponseEntity<>(HttpStatus.CREATED);
		}

		// If one cannot log into the sender's email or if the message fails to be sent
		catch (MailException x) {
			return new ResponseEntity<>(x.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// If the Email/username is already in use
		catch (RegisterException x) {
			return new ResponseEntity<>(x.getMessage(), HttpStatus.BAD_REQUEST);
		}

	}

	// login
	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = LoginResponse.class) })
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UserLoginDTO user) {
		UserEntity ue = userService.loginUser(user);

		// create a new token and store it inside the database
		String token = jwtTokenProvider.createToken(ue.getUsername());
		ue.setToken(token);
		userRepo.save(ue);

		// response, I dont know if this string is converted into a json
		LoginResponse lr = new LoginResponse();
		lr.setAdmin(ue.isAdmin());
		lr.setEmail(ue.getEmail());
		lr.setToken(ue.getToken());
		lr.setUsername(ue.getUsername());
		return new ResponseEntity<>(lr, HttpStatus.OK);

	}

	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = RefreshTokenResponse.class) })
	@GetMapping("/refresh")
	public ResponseEntity<Object> refreshToken(@RequestHeader("Token") String token) {
		UserEntity verified = userRepo.findByToken(token);
		if (verified == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (!verified.isVerified()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		// create a new token and store it inside the database
		token = jwtTokenProvider.createToken(verified.getUsername());
		verified.setToken(token);
		userRepo.save(verified);
		
		RefreshTokenResponse rtr = new RefreshTokenResponse();
		rtr.setToken(token);

		return new ResponseEntity<Object>(rtr, HttpStatus.OK);
	}

	// Verification through an email
	@GetMapping("/regitrationConfirmation")
	public ResponseEntity<?> confirmRegistration(@RequestParam("token") String token) {
		// find a user by the verif. token; if none is found, the user does not exist
		UserEntity verified = userRepo.findByToken(token);
		if (verified == null) {
			return new ResponseEntity<>(ms.getMessage("tokenNotFound", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		if (verified.isVerified()) {
			return new ResponseEntity<>(ms.getMessage("accountAlreadyVerified", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

		// check if the token is expired
		if (jwtTokenProvider.validateToken(token)) {
			return new ResponseEntity<>(ms.getMessage("tokenExpired", null, LocaleContextHolder.getLocale()),
					HttpStatus.UNAUTHORIZED);
		}
		// user is now verified
		verified.setVerified(true);
		userService.saveUser(verified);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/resendConfirmation")
	public ResponseEntity<?> resendConfirmation(@RequestBody UserEmailDTO email) {
		// find the user making the request
		UserEntity ue = userRepo.findByEmail(email.getEmail());
		if (ue == null) {
			return new ResponseEntity<>(ms.getMessage("noAccountWithEmail", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		if (ue.isVerified()) {
			return new ResponseEntity<>(ms.getMessage("accountAlreadyVerified", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		// generate a token and send out the email
		ue.setToken(jwtTokenProvider.createToken(ue.getUsername()));
		userRepo.save(ue);
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(ue.getEmail());
		msg.setSubject(ms.getMessage("emailSubject", null, LocaleContextHolder.getLocale()));

		msg.setText(ms.getMessage("emailBody1", null, LocaleContextHolder.getLocale()) + url
				+ ms.getMessage("emailBody2", null, LocaleContextHolder.getLocale()) + ue.getToken());
		try {
			javaMailSender.send(msg);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MailException x) {
			return new ResponseEntity<>(x.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// TODO add a temporary password field to the entity
	// as it stands, anyone could reset another one's password by just providing the
	// email, so a temporary password
	// not ovewriting the current one would be a fix
	@PostMapping("/resetPassword")
	public ResponseEntity<?> resetPassword(@RequestBody UserEmailDTO email) {
		UserEntity ue = userRepo.findByEmail(email.getEmail());
		System.out.println(email);
		if (ue == null) {
			return new ResponseEntity<>(ms.getMessage("noAccountWithEmail", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		if (!ue.isVerified()) {
			return new ResponseEntity<>(ms.getMessage("accountNotVerified", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		// generate a random password for the user to log in and change later
		String tempPw = generatePassayPassword();
		ue.setPassword(passwordEncoder.encode(tempPw));
		userRepo.save(ue);
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(ue.getEmail());
		msg.setSubject(ms.getMessage("emailResetSubject", null, LocaleContextHolder.getLocale()));

		msg.setText(ms.getMessage("emailResetBody", null, LocaleContextHolder.getLocale()) + tempPw);
		try {
			javaMailSender.send(msg);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MailException x) {
			return new ResponseEntity<>(x.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/changePassword")
	public ResponseEntity<?> updatePassword(@RequestHeader("Token") String token,
			@RequestBody PasswordChangeDTO passwords) {
		UserEntity ue = userRepo.findByToken(token);

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<PasswordChangeDTO>> violations = validator.validate(passwords);
		for (ConstraintViolation<PasswordChangeDTO> violation : violations) {
			return new ResponseEntity<>(violation.getMessage(), HttpStatus.BAD_REQUEST);
		}

		if (ue == null) {
			return new ResponseEntity<>(ms.getMessage("tokenNotFound", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

		// check if the token is expired
		if (jwtTokenProvider.validateToken(token)) {
			return new ResponseEntity<>(ms.getMessage("tokenExpired", null, LocaleContextHolder.getLocale()),
					HttpStatus.UNAUTHORIZED);
		}
		if (!passwordEncoder.matches(passwords.getOldPW(), ue.getPassword())) {
			return new ResponseEntity<>(ms.getMessage("incorrectPW", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		ue.setPassword(passwordEncoder.encode(passwords.getNewPW()));
		userRepo.save(ue);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	// method to generate a password when a user resets his found on baeldung's
	// website
	public String generatePassayPassword() {
		String upperCaseLetters = RandomStringUtils.random(1, 65, 90, true, true);
		String lowerCaseLetters = RandomStringUtils.random(1, 97, 122, true, true);
		String numbers = RandomStringUtils.randomNumeric(1);
		String totalChars = RandomStringUtils.randomAlphanumeric(6);
		String combinedChars = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(totalChars);
		List<Character> pwdChars = combinedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
		Collections.shuffle(pwdChars);
		String password = pwdChars.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
				.toString();
		return password;
	}
	
	@PostMapping("/locale")
	public ResponseEntity<?> updateLanguage(boolean isFrench){
		SessionLocaleResolver slr = new SessionLocaleResolver();
		if(isFrench) {
			slr.setDefaultLocale(Locale.CANADA_FRENCH);
		}
		else {
			slr.setDefaultLocale(Locale.CANADA);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
