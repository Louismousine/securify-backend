package com.services;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dto.UserLoginDTO;
import com.dto.UserRegisterDTO;
import com.entities.UserEntity;
import com.repositories.UserRepository;

/*
 * Service to handle login and registration of users.
 */
@Service
public class UserService {

	@Autowired
	MessageSource ms;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JavaMailSender javaMailSender;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;
	@Value("${baseurl}")
	private String url;

	// register users
	public void addUser(UserRegisterDTO user) {
		
		//check that the email and username are unique
		if (userRepository.findByEmail(user.getEmail()) != null)
			throw new RegisterException(ms.getMessage("emailInUse", null, LocaleContextHolder.getLocale()));
		if (userRepository.findByUsername(user.getUsername()) != null)
			throw new RegisterException(ms.getMessage("usernameInUse", null, LocaleContextHolder.getLocale()));

		//create the user
		UserEntity user1 = new UserEntity();
		user1.setPassword(passwordEncoder.encode(user.getPassword()));
		user1.setEmail(user.getEmail());
		user1.setUsername(user.getUsername());
		String token = jwtTokenProvider.createToken(user1.getUsername());
		user1.setToken(token);
		
		//check if the new user is valid
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<UserEntity>> violations = validator.validate(user1);
		for (ConstraintViolation<UserEntity> violation : violations) {
		    throw new RegisterException(violation.getMessage()); 
		}
		//save it
		userRepository.save(user1);

		// Send email
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom("${spring.mail.username}");
		msg.setTo(user.getEmail());
		msg.setSubject(ms.getMessage("emailSubject", null, LocaleContextHolder.getLocale()));
		msg.setText(ms.getMessage("emailBody1", null, LocaleContextHolder.getLocale())+url+ms.getMessage("emailBody2", null, LocaleContextHolder.getLocale())+ token);
		javaMailSender.send(msg);
	}

	// method that only checks if a user could be logged in
	public UserEntity loginUser(UserLoginDTO user) throws LoginException{
		// if no user is found by its username, it does not exist
		UserEntity user1 = userRepository.findByUsername(user.getUsername());
		if (user1 == null) {
			throw new LoginException(ms.getMessage("usernameNotFound", null, LocaleContextHolder.getLocale()));
		}
		// if the password doesnt match the saved one
		String expectedPW = user1.getPassword();
		if (!passwordEncoder.matches(user.getPassword(), expectedPW)) {
			throw new LoginException(ms.getMessage("incorrectPW", null, LocaleContextHolder.getLocale()));
		}
		// if the user has not verified their account through email
		if (!user1.isVerified())
			throw new LoginException(ms.getMessage("accountNotVerified", null, LocaleContextHolder.getLocale()));
		return user1;

	}

	// save user
	public void saveUser(UserEntity user) {
		userRepository.save(user);

	}
}
