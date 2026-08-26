package config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Builds the Google Drive client bean only. No business logic here - mirrors
 * AwsConfig so both storage providers follow the same "config builds the
 * client, service uses it" split.
 * <p>
 * Uses a service account instead of the original interactive
 * {@code LocalServerReceiver} flow. That flow opened a local browser window to
 * authorize, which cannot work on a headless server/container. Service-account auth needs no user
 * interaction, which is what makes this provider deployable at all.
 * <p>
 */

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "google-drive")
public class GoogleDriveConfig {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_RESOURCE_PATH = "/credentials.json";

    @Value("${application.name:Nimbus Stream}")
    private String applicationName;

    @Bean
    public Drive driveClient() throws GeneralSecurityException, IOException {
        InputStream credentialsStream = GoogleDriveConfig.class.getResourceAsStream(CREDENTIALS_RESOURCE_PATH);
        if(credentialsStream == null){
            throw new FileNotFoundException("Service account credentials not found at " + CREDENTIALS_RESOURCE_PATH +
                    ". Mount it as a resource or set GOOGLE_APPLICATION_CREDENTIALS.");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream).createScoped(Collections.singleton(DriveScopes.DRIVE_READONLY));
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),JSON_FACTORY, requestInitializer).setApplicationName(applicationName).build();

    }

}
