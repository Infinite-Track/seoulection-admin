package com.seoulection.admin.youtube.application.service;

import com.seoulection.admin.youtube.application.dto.ParsedYoutubeChannelUrl;
import com.seoulection.admin.youtube.domain.exception.YoutubeAdminException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Component
public class YoutubeChannelUrlParser {

    public ParsedYoutubeChannelUrl parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw invalid(rawUrl, null);
        }

        String original = rawUrl.trim();
        String valueWithScheme = hasHttpScheme(original) ? original : "https://" + original;
        try {
            URI uri = URI.create(valueWithScheme);
            String host = uri.getHost();
            if (host == null || !isYoutubeHost(host.toLowerCase(Locale.ROOT))) {
                throw invalid(original, null);
            }

            String[] segments = Arrays.stream(uri.getPath().split("/"))
                    .filter(segment -> !segment.isBlank())
                    .map(this::decode)
                    .toArray(String[]::new);
            if (segments.length == 0) {
                throw invalid(original, null);
            }

            String channelName = null;
            String channelId = null;
            String canonicalPath;
            if (segments[0].startsWith("@")) {
                channelName = segments[0];
                canonicalPath = "/" + segments[0];
            } else if (segments.length >= 2 && segments[0].equals("channel")) {
                channelId = segments[1];
                canonicalPath = "/channel/" + channelId;
            } else if (segments.length >= 2
                    && (segments[0].equals("c") || segments[0].equals("user"))) {
                channelName = segments[1];
                canonicalPath = "/" + segments[0] + "/" + channelName;
            } else {
                throw invalid(original, null);
            }

            return new ParsedYoutubeChannelUrl(
                    channelName,
                    channelId,
                    "https://www.youtube.com" + canonicalPath
            );
        } catch (YoutubeAdminException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalid(original, e);
        }
    }

    private boolean isYoutubeHost(String host) {
        return host.equals("youtube.com") || host.endsWith(".youtube.com");
    }

    private boolean hasHttpScheme(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private YoutubeAdminException invalid(String url, Throwable cause) {
        String detail = "유효하지 않은 YouTube 채널 URL url=" + url;
        return cause == null
                ? new YoutubeAdminException(
                YoutubeAdminException.Reason.INVALID_CHANNEL,
                detail
        )
                : new YoutubeAdminException(
                YoutubeAdminException.Reason.INVALID_CHANNEL,
                detail,
                cause
        );
    }
}
