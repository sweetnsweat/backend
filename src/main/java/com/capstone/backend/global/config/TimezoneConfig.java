package com.capstone.backend.global.config;

import com.capstone.backend.global.time.KoreanTime;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimezoneConfig {

    @PostConstruct
    void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(KoreanTime.ZONE_ID));
    }
}
