package com.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

//this class downloads an mp3 of the song found on spotify
public class SpotifyToMP3 {
	
	@Value("${youtube.secretkey}")
// add key here
	private String secretKey="";
	/**
	 * Define a global variable that identifies the name of a file that contains the
	 * developer's API key.
	 */
	@Value("${appname}")
	private String appName="Secretify";
	@Value("${youtube.properties}")
	private String PROPERTIES_FILENAME="youtube.properties";

	// we only want the first result, most likely the wanted search result
	private final long NUMBER_OF_VIDEOS_RETURNED = 1;

	/**
	 * Define a global instance of a Youtube object, which will be used to make
	 * YouTube Data API requests.
	 */
	private YouTube youtube;

	/**
	 * Initialize a YouTube object to search for videos on YouTube. Then display the
	 * name and thumbnail image of each video in the result set.
	 *
	 * @param args command line args.
	 * @throws Exception
	 */
	// as a placeholder for errors i will return a null string
	public String getMP3Link(String keyword) throws Exception {
		// Read the developer key from the properties file.
		Properties properties = new Properties();
		try {
			//get the properties
			InputStream in = SpotifyToMP3.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			return null;
		}

		try {
			// This object is used to make YouTube Data API requests. The last
			// argument is required, but since we don't need anything
			// initialized when the HttpRequest is initialized, we override
			// the interface and provide a no-op function.
			youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
				public void initialize(HttpRequest request) throws IOException {
				}

			}).setApplicationName(appName).build();

			// Define the API request for retrieving search results.
			YouTube.Search.List search = youtube.search().list("id,snippet");
			search.setKey(secretKey);
			search.setQ(keyword);
			search.setType("video");

			// To increase efficiency, only retrieve the fields that the
			// application uses.
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
			search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

			// Call the API and print results.
			SearchListResponse searchResponse = search.execute();
			List<SearchResult> searchResultList = searchResponse.getItems();
			String URL = null;
			if (searchResultList != null) {
				URL = "https://www.youtube.com/watch?v=" + searchResultList.listIterator().next().getId().getVideoId();
				System.out.println(URL);
			}
			//get the path where the song was saved
			String mp3Path = getSongLink(URL, keyword, searchResultList.listIterator().next().getId().getVideoId());
			//return it
			return mp3Path;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			//if no path is found, return null as a sign of expection
			return null;
		}
	}

	// gets a yt link and downloads it as an mp4 then converts it to mp3 then sends
	// the path
	// code sample taken from vget's documentation and tailored to this app's needs
	public String getSongLink(String URL, String songName, String songID) throws Exception {
		if (songName == null)
			return null;
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "youtube-dl -U" + URL);
		Process process = processBuilder.start();
		// download the song to the root dir using cmd comands
		//heroku runs on linux, so the first string has to be "bash" and the second "-c"
		//for windows, use "cmd.exe" and "/c"
		processBuilder = new ProcessBuilder("bash", "-c", "youtube-dl -x --audio-format mp3 " + URL);
		process = processBuilder.start();

		//print out the input from the commandd line
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}

		int exitCode = process.waitFor();
		System.out.println("\nExited with error code : " + exitCode);
//		if (exitCode != 0) {
//			return null;
//		}
		// the mp3 is now downloaded, lets rename it and return its path
		File f1 = new File("./");
		File[] matchingFiles = f1.listFiles(new FilenameFilter() {
			//only retain files taht contain the song id
			public boolean accept(File dir, String name) {
				return name.contains(songID);
			}
		});

		//if a file is found, return its path
		if (matchingFiles.length != 0) {
			return matchingFiles[0].getPath();
		} else {
			return null;
		}

	}
}
