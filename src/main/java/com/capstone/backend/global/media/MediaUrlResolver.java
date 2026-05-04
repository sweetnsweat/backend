package com.capstone.backend.global.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaUrlResolver {

    private static final String MEDIA_PATH_PREFIX = "/media/";

    private final String mediaBaseUrl;

    public MediaUrlResolver(@Value("${app.media.base-url}") String mediaBaseUrl) {
        this.mediaBaseUrl = trimTrailingSlash(mediaBaseUrl);
    }

    public String resolve(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmedUrl = url.trim();
        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            return trimmedUrl;
        }
        if (trimmedUrl.startsWith(MEDIA_PATH_PREFIX)) {
            return mediaBaseUrl + trimmedUrl;
        }
        return trimmedUrl;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmedValue = value.trim();
        while (trimmedValue.endsWith("/")) {
            trimmedValue = trimmedValue.substring(0, trimmedValue.length() - 1);
        }
        return trimmedValue;
    }
}
