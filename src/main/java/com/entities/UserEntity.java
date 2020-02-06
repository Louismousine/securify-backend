package com.entities;

import java.util.ArrayList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Entity
@Data
@Table(name = "user")
public class UserEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Email(message = "{email.invalid}")
	@NotBlank(message = "{email.notempty}")
	@NotNull(message = "{email.notempty}")
	@Column(unique = true)
	private String email;

	@NotNull(message = "{password.notempty}")
	@NotBlank(message = "{password.notempty}")
	private String password;


	@NotEmpty
	@Size(min = 4, max = 20, message="{username.size}")
	private String username;

	private boolean isVerified = false;
	
	//by design, there is no way to become an admin other than to modify the DB
	private boolean isAdmin = false;
	
	private String token;
	
	//songs the user has played in the past
	//would be best to have a list of TrackEntities instead of their IDs
	@Column(length=5000)
	private ArrayList<TrackEntity> songs = new ArrayList<>();

	public void addSongIDs(TrackEntity track) {
		if (songs == null)
			songs = new ArrayList<>();
		songs.add(track);
	}
	public void removeSongId(TrackEntity track) {
		
		songs.remove(track);
	}
}
