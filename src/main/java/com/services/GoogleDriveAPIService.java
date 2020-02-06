package com.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

//this class uploads a song to googledrive and sends back the drive link where it is stored
@Component
public class GoogleDriveAPIService {

	@Value("${appname}")
	private String APP_NAME;
	//private static String APPLICATION_NAME;
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private final String TOKENS_DIRECTORY_PATH = "tokens";

	@Autowired
	private void setMessageSource(MessageSource messageSource) {
		ms = messageSource;
	}
	
	private MessageSource ms;
	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private final List<String> SCOPES = new ArrayList<>();
	private final String CREDENTIALS_FILE_PATH = "/credentials.json";

	// code taken from google api tutorial
	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = GoogleDriveAPIService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException(
					ms.getMessage("resourceNotFound", null, LocaleContextHolder.getLocale()) + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// only manual addition
		SCOPES.add(DriveScopes.DRIVE);
		SCOPES.add(DriveScopes.DRIVE_FILE);

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	//upload the song, return where it was saved
	public String upload(String name, String path) throws GeneralSecurityException, IOException {
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APP_NAME).build();
		// define the songs to be available to read to anyone with the link
		String permissionType = "anyone";
		String permissionRole = "reader";
		Permission newPermission = new Permission();
		newPermission.setType(permissionType);
		newPermission.setRole(permissionRole);

		// create the file with the song in it
		File fileMetadata = new File();
		fileMetadata.setName(name);

		java.io.File filePath = new java.io.File(path);
		FileContent mediaContent = new FileContent("audio/mpeg", filePath);
		// upload the file and get its ID back
		File file = service.files().create(fileMetadata, mediaContent).setFields("id").execute();
		System.out.println("File ID: " + file.getId());
		// set the song to be public
		service.permissions().create(file.getId(), newPermission).setFields("id").execute();
		// retrieve the download link
		File file1 = service.files().get(file.getId()).setFields("webContentLink").execute();
		System.out.println(file1.getWebContentLink());
		return file1.getWebContentLink();

	}
}
