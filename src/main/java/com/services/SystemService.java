package com.services;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.dto.GetSongDTO;
import com.dto.PlaylistDTO;
import com.dto.PositionDto;
import com.dto.SongSystemDTO;
import com.entities.PlaylistMetadataEntity;
import com.entities.TrackEntity;
import com.repositories.SystemRepository;
import com.repositories.TrackRepository;

//service to govern the system requests
@Service
@Transactional
public class SystemService {

	@Autowired
	private TrackRepository trackRepo;

	@Autowired
	private SystemRepository systemRepo;

	@Autowired
	private MessageSource ms;

	// edit the system configs
	public ResponseEntity<?> editSystem(SongSystemDTO system) {
		// make sure there is a SystemEntity in the db
		initializeSystemIfNull();
		// get the SystemEntity
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);

		// error message
		String error = null;

		System.out.println(system.getPositionInPlaylist());
		// the following chained if/elses will edit the systementity
		// if one of the input is invalid, the response will be a 400

		TrackEntity te = trackRepo.findByPosition(system.getPositionInPlaylist());
		if (system.getSecondsPlayed() != null && system.getSecondsPlayed() >= 0
				&& system.getSecondsPlayed() < te.getLengthSeconds()) {
			se.setSecondsPlayed(system.getSecondsPlayed());
		} else {
			error = ms.getMessage("runtimePositive", null, LocaleContextHolder.getLocale());

		}
		if (system.getPositionInPlaylist() >= 0
				&& system.getPositionInPlaylist() < trackRepo.count()) {
			se.setPositionInPlaylist(system.getPositionInPlaylist());

			// if the song was changed, we restart at 0
			se.setSecondsPlayed(0);
		} else {
			if (error == null) {
				error = ms.getMessage("playlistPositionPositive", null, LocaleContextHolder.getLocale());
			} else {
				error += "; " + ms.getMessage("playlistPositionPositive", null, LocaleContextHolder.getLocale());
			}
		}

		if (error != null)
			return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);

		// if no error was found, save the entity

		systemRepo.save(se);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// send the next song
	public ResponseEntity<GetSongDTO> getCurrentSong() {
		// make sure there is a SystemEntity in the db
		initializeSystemIfNull();
		// get the system config
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);
		// find the song
		TrackEntity te = trackRepo.findByPosition(se.getPositionInPlaylist());
		// create a header with the time where the song was left off
		GetSongDTO getSong = new GetSongDTO();
		getSong.setSecondsPlayed(se.getSecondsPlayed());
		getSong.setTrack(te);
		return new ResponseEntity<>(getSong, HttpStatus.OK);
	}

	// send the next song
	public ResponseEntity<?> getNextSong() {
		// make sure there is a SystemEntity in the db
		initializeSystemIfNull();
		// get the system config
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);

		TrackEntity te = null;
		// if there is another song to be played after the current one
		if (trackRepo.existsByPosition(se.getPositionInPlaylist() + 1)) {
			// then get the song, and save the systementity
			te = trackRepo.findByPosition(se.getPositionInPlaylist() + 1);
			se.setPositionInPlaylist(se.getPositionInPlaylist() + 1);
			se.setSecondsPlayed(0);
			systemRepo.save(se);
		}
		// if no song was found, there is no next song, bad request
		if (te == null) {
			return new ResponseEntity<>(ms.getMessage("endPlaylist", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		// send the time when the song was left off (0)
		GetSongDTO getSong = new GetSongDTO();
		getSong.setSecondsPlayed(se.getSecondsPlayed());
		getSong.setTrack(te);
		return new ResponseEntity<>(getSong, HttpStatus.OK);
	}

	// send the previous song
	public ResponseEntity<?> getPreviousSong() {
		// make sure there is a SystemEntity in the db
		initializeSystemIfNull();
		// get the system config
		PlaylistMetadataEntity se = systemRepo.findAll().get(0);
		TrackEntity te = null;
		// if there is another song to be played before the current one
		if (trackRepo.existsByPosition(se.getPositionInPlaylist() - 1)) {
			te = trackRepo.findByPosition(se.getPositionInPlaylist() - 1);
			se.setPositionInPlaylist(se.getPositionInPlaylist() - 1);
			se.setSecondsPlayed(0);
			systemRepo.save(se);
		}
		// if no song was found, bad request, there is no previous song
		if (te == null) {
			return new ResponseEntity<>(ms.getMessage("noPrevSong", null, LocaleContextHolder.getLocale()),
					HttpStatus.BAD_REQUEST);
		}
		GetSongDTO getSong = new GetSongDTO();
		getSong.setSecondsPlayed(se.getSecondsPlayed());
		getSong.setTrack(te);
		return new ResponseEntity<>(getSong, HttpStatus.OK);
	}

	// initialize a systementity if none exist
	private void initializeSystemIfNull() {
		if (systemRepo.count() == 0) {
			PlaylistMetadataEntity se = new PlaylistMetadataEntity();
			se.setSecondsPlayed(0);
			se.setPositionInPlaylist(0);
			systemRepo.save(se);
		}
	}

	public ResponseEntity<?> setPosition(PositionDto pos) {
		// make sure there is a SystemEntity in the db
				initializeSystemIfNull();
				// get the system config
				PlaylistMetadataEntity se = systemRepo.findAll().get(0);

				System.out.println(pos.getPosition());
				// if there is a song with the specified position
				if (trackRepo.existsByPosition(pos.getPosition())) {
					// then get the song, and save the systementity
					se.setPositionInPlaylist(pos.getPosition());
					System.out.println("here");
					se.setSecondsPlayed(0);
					systemRepo.save(se);
				}
				// if no song was found, there is no next song, bad request
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
}
