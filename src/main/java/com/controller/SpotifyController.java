package com.controller;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.dto.PlaylistDTO;
import com.dto.SearchedTrack;
import com.dto.SongIDDTO;
import com.entities.PlaylistMetadataEntity;
import com.entities.TrackEntity;
import com.entities.UserEntity;
import com.neovisionaries.i18n.CountryCode;
import com.repositories.SystemRepository;
import com.repositories.TrackRepository;
import com.repositories.UserRepository;
import com.services.GoogleDriveAPIService;
import com.services.JwtTokenProvider;
import com.services.SpotifyToMP3;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

//this class handles requests meant to get and edit the playlist items
//and to add items to it
@Validated
@RestController
public class SpotifyController {
	@Value("${clientId}")
	private String clientId;
	@Value("${clientSecret}")
	private String clientSecret;
	private SpotifyApi spotifyApi;
	private ClientCredentialsRequest clientCredentialsRequest;


	// number of songs popping up in a search
	@Value("${maxsearchresults}")
	private Integer MAX_NUMBER_OF_SONGS;

	@Autowired
	private MessageSource ms;
	@Autowired
	private SystemRepository systemRepo;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private TrackRepository trackRepo;
	
	// search songs
	// for the sake of this app, i will only return a predefined number of songs
	// matching the keyword sent (NUMBER_OF_SONGS)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = SearchedTrack.class) })
	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> searchForSongs(@RequestParam("q") String keyword, @RequestHeader("Token") String token) {
		//make sure the token holder is a valid user and that the token is valid
		ResponseEntity<?> re = validateUser(token);
		if (re != null) {
			return re;
		}
		try {
			// get search result, extract the tracks and return them in jsons
			Paging<Track> SI = spotifyApi.searchTracks(keyword).market(CountryCode.CA).build().execute();
			if (SI == null) {
				return new ResponseEntity<>(ms.getMessage("noResults", null, LocaleContextHolder.getLocale()), HttpStatus.OK);}
			Track[] tracks = SI.getItems();
			//the JSONs of the songs that match the search keywords
			ArrayList<SearchedTrack> jsons = new ArrayList<SearchedTrack>();
			for (int i = 0; jsons.size() < MAX_NUMBER_OF_SONGS && i < tracks.length; i++) {
				// filter out explicit songs
				if (!tracks[i].getIsExplicit() && tracks[i].getIsPlayable()) {
					jsons.add(buildSearchedTrack(tracks[i]));
				}
			}

			return new ResponseEntity<>(jsons, HttpStatus.OK);

		} catch (SpotifyWebApiException | IOException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	//add a song tgat appeared in the search
	@PostMapping("/search")
	public ResponseEntity<?> addSong(@RequestBody SongIDDTO song, @RequestHeader("Token") String token) {
		//make sure the token holder is a valid user and that the token is valid
		ResponseEntity<?> re = validateUser(token);
		if (re != null) {
			return re;
		}
		PlaylistMetadataEntity se = null;
		UserEntity ue = userRepo.findByToken(token);
		// now lets check if the user has already added a song to the playlist
		//if the user is an admin, he is allowed to add as many songs as he pleases,so this check doesnt apply to him
		if (!ue.isAdmin()) {
			TrackEntity te = trackRepo.findByUserid(ue.getId());
			if (te != null) {
				//the user is not an admin and he has already added a song, he cannot add another one, 400
				return new ResponseEntity<>(ms.getMessage("userDailyLimit", null, LocaleContextHolder.getLocale()), HttpStatus.BAD_REQUEST);
			}
		}
		// check if the song has already been played/is already in the queue
		TrackEntity te = trackRepo.findBySongid(song.getId());
		if (te != null && te.getPosition() != null) {
			return new ResponseEntity<>(ms.getMessage("songDailyLimit", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

		// if it has already been played, but not today, the google drive should still
		// have the song
		// so we just need to add the trac kto the playlist
		if (te != null) {
			//if no system entity exists, create one
			if (systemRepo.count() == 0) {
				se = new PlaylistMetadataEntity();
				se.setSecondsPlayed(0);
				se.setPositionInPlaylist(0);
				systemRepo.save(se);
			}
			//add the song to the queue, save it, 200
			se = systemRepo.findAll().get(0);
			List<TrackEntity> list = trackRepo.findAll();
			int numOfSongs = 0;
			for(TrackEntity t: list) {
				if(t.getPosition()!=null)
					numOfSongs++;
			}
			te.setPosition(numOfSongs);
			se.setSongsInPlaylist(numOfSongs+ 1);
			systemRepo.save(se);
			trackRepo.save(te);
			List<TrackEntity> tes = trackRepo.findAll(Sort.by(Sort.Direction.ASC, "position"));
			List<TrackEntity> teReturn = new ArrayList<>();
			for(TrackEntity t: tes) {
				if(t.getPosition()!=null) {
					teReturn.add(t);
				}
			}
			PlaylistDTO pdto = new PlaylistDTO();
			pdto.setPosition(se.getPositionInPlaylist());
			pdto.setSecondsPlayed(se.getSecondsPlayed());
			pdto.setTrackEntities(teReturn);
			return new ResponseEntity<>(pdto, HttpStatus.OK);
		}

		// if we get to here, the user can add a song, but the song is not on the google drive
		// we shall add the song to the queue and to the user's list of played songs
		try {
			// get search result, extract the tracks and return them in jsons
			Track track = spotifyApi.getTrack(song.getId()).market(CountryCode.CA).build().execute();
			if (track == null) {
				return new ResponseEntity<>(ms.getMessage("songNotFound", null, LocaleContextHolder.getLocale()),
						HttpStatus.BAD_REQUEST);
			}

			// filter out explicit and unplayable songs
			if (!track.getIsExplicit() && track.getIsPlayable()) {

				// build the track objects
				te = buildTrackEntity(track, ue, song.getId());

				// the next brackets enclose the part where the mp3 is downloaded locally to
				// youtube
				// then uploaded to drive
				{
					String mp3Link = null;
					try {
						//download the song
						SpotifyToMP3 spotifee = new SpotifyToMP3();
						mp3Link = spotifee.getMP3Link(track.getName() + " " + track.getArtists()[0].getName());
						System.out.println(mp3Link);
					} catch (Exception e) {
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
					if (mp3Link == null) {
						return new ResponseEntity<>(
								ms.getMessage("downloadFailed", null, LocaleContextHolder.getLocale()),
								HttpStatus.INTERNAL_SERVER_ERROR);
					}
					
					//upload it
					GoogleDriveAPIService gs = new GoogleDriveAPIService();
					te.setMp3Link(gs.upload(track.getName(), mp3Link));
					//remove the local file
					File f = new File(mp3Link);
					f.delete();
				}

				
				//if no system entity exists, create one
				if (systemRepo.count() == 0) {
				se = new PlaylistMetadataEntity();
					se.setSecondsPlayed(0);
					se.setPositionInPlaylist(0);
					systemRepo.save(se);
				}
				//add the song to the queue, save it
				se = systemRepo.findAll().get(0);
				List<TrackEntity> list = trackRepo.findAll();
				int numOfSongs = 0;
				for(TrackEntity t: list) {
					if(t.getPosition()!=null)
						numOfSongs++;
				}
				te.setPosition(numOfSongs);
				se.setSongsInPlaylist(numOfSongs+ 1);
				systemRepo.save(se);
				// save the track
				trackRepo.save(te);

				// save the song id to the list of songs added by the user
				if (!ue.getSongs().contains(te) && !ue.isAdmin()) {
					ue.addSongIDs(te);
					userRepo.save(ue);
				}
			}
			List<TrackEntity> tes = trackRepo.findAll(Sort.by(Sort.Direction.ASC, "position"));
			List<TrackEntity> teReturn = new ArrayList<>();
			for(TrackEntity t: tes) {
				if(t.getPosition()!=null) {
					teReturn.add(t);
				}
			}
			PlaylistDTO pdto = new PlaylistDTO();
			pdto.setPosition(se.getPositionInPlaylist());
			pdto.setSecondsPlayed(se.getSecondsPlayed());
			pdto.setTrackEntities(teReturn);
			return new ResponseEntity<>(pdto, HttpStatus.OK);

		} catch (SpotifyWebApiException | IOException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (GeneralSecurityException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// method to send all of the tracks that are queued in the db
	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = PlaylistDTO.class)})
	@RequestMapping(value = "/playlist", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<?> getPlaylist(@RequestHeader("Token") String token) {
		//make sure the user making the request exists
		UserEntity ue = userRepo.findByToken(token);
		if (ue == null) {
			return new ResponseEntity<>(ms.getMessage("tokenNotFound", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

		// check if the token is expired
		if (jwtTokenProvider.validateToken(token)) {
			return new ResponseEntity<>(ms.getMessage("tokenExpired", null, LocaleContextHolder.getLocale()),
					HttpStatus.UNAUTHORIZED);
		}
		// get all tracks sorted by id (position in playlist)
		List<TrackEntity> tes = trackRepo.findAll(Sort.by(Sort.Direction.ASC, "position"));

		// if no system config exists, create it here
		PlaylistMetadataEntity se;
		if (systemRepo.count() == 0) {
			se = new PlaylistMetadataEntity();
			se.setSecondsPlayed(0);
			se.setPositionInPlaylist(0);
			systemRepo.save(se);
		}
		se = systemRepo.findAll().get(0);
		List<TrackEntity> teReturn = new ArrayList<>();
		for(TrackEntity t: tes) {
			if(t.getPosition()!=null) {
				teReturn.add(t);
			}
		}
		PlaylistDTO pdto = new PlaylistDTO();
		pdto.setPosition(se.getPositionInPlaylist());
		pdto.setSecondsPlayed(se.getSecondsPlayed());
		pdto.setTrackEntities(teReturn);
		return new ResponseEntity<>(pdto, HttpStatus.OK);
	}

	
	// delete a song from the playlist
	//a user can delete his own added song; an admin can delete any
	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = PlaylistDTO.class)})
	@RequestMapping(value = "/playlist/{SongId}", method = RequestMethod.DELETE)
	public ResponseEntity<?> removeSongFromPlaylist(@RequestHeader("Token") String token,
			@PathVariable(value = "SongId") String SongID) {
		//validate the user
		ResponseEntity<?> re = validateUser(token);
		if (re != null) {
			return re;
		}

		UserEntity ue = userRepo.findByToken(token);

		// now lets check if the user has already added a song to the playlist
		TrackEntity te = trackRepo.findBySongid(SongID);
		int position = te.getPosition();
		System.out.println(te.getPosition());
		System.out.println(position);

		// if the user is an dmin, he can delete any song. no validation needed
		if (!ue.isAdmin()) {
			// the song must have been added by the user making the request
			if (te == trackRepo.findByUserid(ue.getId()))
				return new ResponseEntity<>(ms.getMessage("wrongUser", null, LocaleContextHolder.getLocale()),
						HttpStatus.BAD_REQUEST);
		}
		List<TrackEntity> tes = trackRepo.findAll(Sort.by(Sort.Direction.ASC, "position"));
		for(TrackEntity t: tes) {
			if(t.getPosition() == te.getPosition()) {
				t.setPosition(null);
				trackRepo.save(t);
			}
			if( t.getPosition()!=null && t.getPosition()>position) {
				t.setPosition(t.getPosition()-1);
				trackRepo.save(t);
			}
		}
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);
		se.setSongsInPlaylist(se.getSongsInPlaylist()-1);
		if(position < se.getPositionInPlaylist()) {
			se.setPositionInPlaylist(se.getPositionInPlaylist()-1);
		}
		systemRepo.save(se);
		tes = trackRepo.findAll(Sort.by(Sort.Direction.ASC, "position"));
		List<TrackEntity> teReturn = new ArrayList<>();
		for(TrackEntity t: tes) {
			if(t.getPosition()!=null) {
				teReturn.add(t);
			}
		}
		
		PlaylistDTO pdto = new PlaylistDTO();
		pdto.setPosition(se.getPositionInPlaylist());
		pdto.setTrackEntities(teReturn);
		pdto.setSecondsPlayed(0);
		return new ResponseEntity<>(pdto, HttpStatus.OK);
	}

	// remove songs from list of users previously queued songs
	// admin has no privilege here
	@RequestMapping(value = "/mysongs", method = RequestMethod.DELETE)
	public ResponseEntity<?> removeSongFromUsersTracks(@RequestHeader("Token") String token,
			@RequestBody SongIDDTO songID) {

		//validate the user
		ResponseEntity<?> re = validateUser(token);
		if (re != null) {
			return re;
		}
		UserEntity ue = userRepo.findByToken(token);

		// now lets check if the user has already added a song to the playlist
		TrackEntity te = trackRepo.findBySongid(songID.getId());
		if (ue.getSongs().contains(te) && te != null) {
			ue.removeSongId(te);
			userRepo.save(ue);
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			return new ResponseEntity<>(ms.getMessage("songNotFound", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

	}

	// get the songs the user has prev queued
	@ApiResponses(value = { @ApiResponse(code = 200, message = "", response = SearchedTrack.class) })
	@RequestMapping(value = "/mysongs", method = RequestMethod.GET)
	public ResponseEntity<?> getSongFromUsersTracks(@RequestHeader("Token") String token) {
		//validate the user
		ResponseEntity<?> re = validateUser(token);
		if (re != null) {
			return re;
		}
		UserEntity ue = userRepo.findByToken(token);

		// list of song ids the user queued
		ArrayList<TrackEntity> ids = ue.getSongs();
		// list to be returned
		ArrayList<SearchedTrack> tes = new ArrayList<>();
		try {
			// add the songs to the list
			for (TrackEntity x : ids) {
				Track track = spotifyApi.getTrack(x.getSongid()).market(CountryCode.CA).build().execute();
				if (track != null) {
					tes.add(buildSearchedTrack(track));
				}
			}
			// return the list
			return new ResponseEntity<>(tes, HttpStatus.OK);
		} catch (SpotifyWebApiException | IOException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// TODO: figure how how to handle exceptions thrown here
	// renews the access token every 60 mins
	@Scheduled(fixedRate = 1000 * 60 * 60)
	public void clientCredentials_Sync() {
		try {
			spotifyApi = new SpotifyApi.Builder().setClientId(clientId)
					.setClientSecret(clientSecret).build();
			 clientCredentialsRequest = spotifyApi.clientCredentials().build();
			// get a new token
			final ClientCredentials clientCredentials = clientCredentialsRequest.execute();

			// Set access token for further "spotifyApi" object usage
			spotifyApi.setAccessToken(clientCredentials.getAccessToken());

			System.out.println("Expires in: " + clientCredentials.getExpiresIn());
		} catch (IOException | SpotifyWebApiException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	// clears the playlist everyday (set the position in the playlist to null; at
	// 1:01 (server side)
	@Scheduled(cron = "0 1 1 * * ?")
	public void resetPlaylist() {
		List<TrackEntity> tes = trackRepo.findAll();
		for (TrackEntity te : tes) {
			te.setPosition(null);
			te.setUserid(null);
			trackRepo.save(te);
		}
		if (systemRepo.count() == 0) {
			PlaylistMetadataEntity se = new PlaylistMetadataEntity();
			se.setSecondsPlayed(0);
			se.setPositionInPlaylist(0);
			systemRepo.save(se);
		}
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);
		se.setSongsInPlaylist(0);
		se.setPositionInPlaylist(0);
		systemRepo.save(se);
	}
	
	private void deleteAllDriveFilesAndResetPlaylist() {
		trackRepo.deleteAll();
		GoogleDriveAPIService drive = new GoogleDriveAPIService();
	}

	// helper to build a track object
	private SearchedTrack buildSearchedTrack(Track track) {
		SearchedTrack st = new SearchedTrack();
		st.setAlbum(track.getAlbum().getName());
		st.setArtistName(track.getArtists()[0].getName());
		st.setId(track.getId());
		st.setLengthSeconds(track.getDurationMs() / 1000);
		st.setName(track.getName());
		st.setImageURL(track.getAlbum().getImages()[0].getUrl());
		return st;
	}

	// helper to build a track entity
	private TrackEntity buildTrackEntity(Track track, UserEntity ue, String songID)
			throws SpotifyWebApiException, IOException {
		TrackEntity te = new TrackEntity();
		te.setAlbum(track.getAlbum().getName());
		te.setSongid(songID);
		te.setImageURL(track.getAlbum().getImages()[0].getUrl());
		te.setLengthSeconds(track.getDurationMs() / 1000);
		te.setName(track.getName());

		if (systemRepo.count() == 0) {
			PlaylistMetadataEntity se = new PlaylistMetadataEntity();
			se.setSecondsPlayed(0);
			se.setPositionInPlaylist(0);
			systemRepo.save(se);
		}
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);
		List<TrackEntity> list = trackRepo.findAll();
		int numOfSongs = 0;
		for(TrackEntity t: list) {
			if(t.getPosition()!=null)
				numOfSongs++;
		}
		te.setPosition(numOfSongs);
		se.setSongsInPlaylist(numOfSongs+ 1);
		systemRepo.save(se);
		if (!ue.isAdmin())
			te.setUserid(ue.getId());
		te.setArtistName(track.getArtists()[0].getName());
		return te;
	}

	// check if the token is valid and that it belongs to a user
	private ResponseEntity<?> validateUser(String token) {
		// check if the token is expired
		if (jwtTokenProvider.validateToken(token)) {
			return new ResponseEntity<>(ms.getMessage("tokenExpired", null, LocaleContextHolder.getLocale()),
					HttpStatus.UNAUTHORIZED);
		}
		// make sure a user has the token
		UserEntity ue = userRepo.findByToken(token);
		if (ue == null) {
			return new ResponseEntity<>(ms.getMessage("tokenNotFound", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}

		return null;
	}

}
