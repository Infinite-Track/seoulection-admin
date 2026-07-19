package com.seoulection.admin.youtube.application.dto;

public record ParsedYoutubeUrl(
        String videoId,
        String originalUrl,
        String canonicalUrl
) {
}
