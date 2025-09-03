package com.groomthon.habiglow.global.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${fcm.firebase.config.path:}")
    private String credentialsPath;   // ex) classpath:... 또는 file:/...

    @Bean
    public FirebaseApp firebaseApp(ResourceLoader loader) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
        if (!StringUtils.hasText(credentialsPath)) {
            throw new IllegalStateException(
                    "fcm.firebase.config.path 를 classpath:/... 또는 file:/... 로 설정하세요."
            );
        }
        try (InputStream in = loader.getResource(credentialsPath).getInputStream()) {
            return FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(in)) // JSON의 project_id 자동 사용
                            .build()
            );
        }
    }
}
