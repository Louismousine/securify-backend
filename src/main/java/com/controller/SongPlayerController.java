package com.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.dto.PlaylistDTO;
import com.dto.PositionDto;
import com.dto.SearchedTrack;
import com.dto.SongSystemDTO;
import com.entities.UserEntity;
import com.repositories.UserRepository;
import com.services.JwtTokenProvider;
import com.services.SystemService;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

//this controller allows to edit the playlist (volume, ordering and time when song starts)
@RestController
public class SongPlayerController {

	@Autowired
	private MessageSource ms;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private SystemService sysServ;

	// this method edits any of the system's settings
	// To edit the settings, pass to this method a JSON containing a mute field
	// and any other field from SongSystemDTO that you want to be changed
	// for instance, to play the 5th song in the playlist, input positionInPlaylist
	// as 5
	@RequestMapping(value = "/playlist", method = RequestMethod.PUT, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> editSystem(@RequestBody SongSystemDTO system, @RequestHeader("Token") String token) {
		//make sure the one making the request is a valid admin
		ResponseEntity<?> re = validateAdmin(token);
		if(re != null)
			return re;
		return sysServ.editSystem(system);
	}

	//gets the song that is to be played now
	@RequestMapping(value = "/current", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> getCurrentSong(@RequestHeader("Token") String token) {
		//make sure the one making the request is a valid admin
		ResponseEntity<?> re = validateAdmin(token);
		if(re != null)
			return re;
		return sysServ.getCurrentSong();
	}

	// this is used to play the next song. as such, the timer will be reset
	@RequestMapping(value = "/next", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> getNextSong(@RequestHeader("Token") String token) {
		//make sure the one making the request is a valid admin
		ResponseEntity<?> re = validateAdmin(token);
		if(re != null)
			return re;
		return sysServ.getNextSong();
	}

	// this is used to play the previous song. as such, the timer will be reset
	@RequestMapping(value = "/previous", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> getPrevSong(@RequestHeader("Token") String token) {
		//make sure the one making the request is a valid admin
		ResponseEntity<?> re = validateAdmin(token);
		if(re != null)
			return re;

		return sysServ.getPreviousSong();
	}
	
	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = PlaylistDTO.class) })
	@RequestMapping(value = "/editPosition", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> changeSongPosition(@RequestHeader("Token") String token, @RequestBody PositionDto pos) {
		//make sure the one making the request is a valid admin
		ResponseEntity<?> re = validateAdmin(token);
		if(re != null)
			return re;

		return sysServ.setPosition(pos);
	}

	//this method checks if the user is a valid user, an admin, and that his token is still valid
	private ResponseEntity<?> validateAdmin(String token) {
		// check if the token is expired
		if (jwtTokenProvider.validateToken(token)) {
			return new ResponseEntity<>(ms.getMessage("tokenExpired", null, LocaleContextHolder.getLocale()),
					HttpStatus.UNAUTHORIZED);
		}
		// check if token is someone's
		UserEntity ue = userRepo.findByToken(token);
		if (ue == null) {
			return new ResponseEntity<>(ms.getMessage("tokenNotFound", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

		// that someone has to be an admin
		if (!ue.isAdmin()) {
			return new ResponseEntity<>(ms.getMessage("notAdmin", null, LocaleContextHolder.getLocale()), HttpStatus.BAD_REQUEST);}
		return null;
	}

}
