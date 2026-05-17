package com.capstone.backend.notification.config;

import com.capstone.backend.notification.service.FcmPushNotificationSender;
import com.capstone.backend.notification.service.NoopPushNotificationSender;
import com.capstone.backend.notification.service.PushNotificationSender;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseMessagingConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        GoogleCredentials credentials = firebaseCredentials(properties);
        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(credentials);
        if (StringUtils.hasText(properties.projectId())) {
            optionsBuilder.setProjectId(properties.projectId().trim());
        }
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (FirebaseApp.DEFAULT_APP_NAME.equals(app.getName())) {
                return app;
            }
        }
        return FirebaseApp.initializeApp(optionsBuilder.build());
    }

    @Bean
    @ConditionalOnBean(FirebaseApp.class)
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnBean(FirebaseMessaging.class)
    public PushNotificationSender fcmPushNotificationSender(FirebaseMessaging firebaseMessaging) {
        return new FcmPushNotificationSender(firebaseMessaging);
    }

    @Bean
    @ConditionalOnMissingBean(PushNotificationSender.class)
    public PushNotificationSender noopPushNotificationSender() {
        return new NoopPushNotificationSender();
    }

    private GoogleCredentials firebaseCredentials(FirebaseProperties properties) throws IOException {
        if (!StringUtils.hasText(properties.serviceAccountPath())) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream inputStream = new FileInputStream(properties.serviceAccountPath().trim())) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }
}
