package pe.smartcash.cash.transactions.infrastructure.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Solo se activa si app.fcm.enabled=true; en dev/test la app arranca sin necesitar un proyecto Firebase real. */
@Configuration
@ConditionalOnProperty(prefix = "app.fcm", name = "enabled", havingValue = "true")
class FirebaseConfig {

  @Bean
  FirebaseApp firebaseApp(@Value("${app.fcm.credentials-path}") String credentialsPath) throws IOException {
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }
    try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
      FirebaseOptions options =
          FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
      return FirebaseApp.initializeApp(options);
    }
  }

  @Bean
  FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
    return FirebaseMessaging.getInstance(firebaseApp);
  }
}
