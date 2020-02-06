# Securify

Backend for music collaborative music streaming service. Uses spotify, google drive, youtube and gmail APIs. Made in June 2019



Placeholder readme with requirements for the B-E to run.

1. Project Lombok must be installed and added to the IDE running the program https://projectlombok.org/download
2. youtube-dl must be downloaded and added to the path https://ytdl-org.github.io/youtube-dl/download.html - then, reboot

Other configurations:

1. Email - first, one needs to "allow less secure apps" to gain access to one's google account. Then, one must enter their email and password in the application.properties file.
2. Spotify - https://developer.spotify.com/dashboard/ login here to get a clientId and secret to then replace in application.properties
3. Youtube - get secret key here https://console.developers.google.com/apis/credentials

The API documentation can be found here http://localhost:8090/swagger-ui.html#/ (when the application is ran locally)

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
