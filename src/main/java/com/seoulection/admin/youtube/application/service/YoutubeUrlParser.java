package com.seoulection.admin.youtube.application.service;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeUrl;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class YoutubeUrlParser {

    private static final Pattern VIDEO_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{6,64}$");

    public ParsedYoutubeUrl parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw invalid(rawUrl, null);
        }

        String originalUrl = rawUrl.trim();
        String valueWithScheme = hasHttpScheme(originalUrl)
                ? originalUrl
                : "https://" + originalUrl;

        try {
            URI uri = URI.create(valueWithScheme);
            String host = uri.getHost();
            if (host == null) {
                throw invalid(originalUrl, null);
            }

            host = host.toLowerCase(Locale.ROOT);
            String videoId;
            if (host.equals("youtu.be") || host.endsWith(".youtu.be")) {
                videoId = firstPathSegment(uri.getPath());
            } else if (isYoutubeHost(host)) {
                videoId = extractFromYoutubeUri(uri);
            } else {
                throw invalid(originalUrl, null);
            }

            videoId = decode(videoId);
            if (videoId == null || !VIDEO_ID_PATTERN.matcher(videoId).matches()) {
                throw invalid(originalUrl, null);
            }

            return new ParsedYoutubeUrl(
                    videoId,
                    originalUrl,
                    "https://www.youtube.com/watch?v=" + videoId
            );
        } catch (YoutubeAdminException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalid(originalUrl, e);
        }
    }

    private String extractFromYoutubeUri(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.equals("/watch") || path.equals("/watch/")) {
            return queryParameter(uri.getRawQuery(), "v");
        }

        String[] segments = Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .toArray(String[]::new);
        if (segments.length >= 2
                && (segments[0].equals("shorts")
                || segments[0].equals("embed")
                || segments[0].equals("live"))) {
            return segments[1];
        }
        return null;
    }

    private String queryParameter(String rawQuery, String name) {
        if (rawQuery == null) {
            return null;
        }
        return Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parts -> parts.length == 2 && decode(parts[0]).equals(name))
                .map(parts -> decode(parts[1]))
                .findFirst()
                .orElse(null);
    }

    private String firstPathSegment(String path) {
        if (path == null) {
            return null;
        }
        return Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .findFirst()
                .orElse(null);
    }

    private boolean isYoutubeHost(String host) {
        return host.equals("youtube.com")
                || host.endsWith(".youtube.com")
                || host.equals("youtube-nocookie.com")
                || host.endsWith(".youtube-nocookie.com");
    }

    private boolean hasHttpScheme(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String decode(String value) {
        return value == null
                ? null
                : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private YoutubeAdminException invalid(String url, Throwable cause) {
        String detail = "유효하지 않은 YouTube URL url=" + url;
        return cause == null
                ? new YoutubeAdminException(
                YoutubeAdminException.Reason.INVALID_VIDEO_URL,
                detail
        )
                : new YoutubeAdminException(
                YoutubeAdminException.Reason.INVALID_VIDEO_URL,
                detail,
                cause
        );
    }
}
