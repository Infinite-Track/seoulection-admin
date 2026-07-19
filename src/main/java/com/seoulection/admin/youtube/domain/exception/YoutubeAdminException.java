package com.seoulection.admin.youtube.domain.exception;

public class YoutubeAdminException extends RuntimeException {

    public enum Reason {
        INVALID_CHANNEL("올바른 YouTube 채널 정보를 입력해 주세요."),
        DUPLICATE_CHANNEL("이미 등록된 YouTube 채널입니다."),
        YOUTUBER_NOT_FOUND("등록된 YouTube 채널을 찾을 수 없습니다."),
        INVALID_VIDEO_URL("올바른 YouTube 영상 URL을 입력해 주세요."),
        DUPLICATE_VIDEO("이미 등록된 YouTube 영상입니다.");

        private final String userMessage;

        Reason(String userMessage) {
            this.userMessage = userMessage;
        }

        public String userMessage() {
            return userMessage;
        }
    }

    private final Reason reason;

    public YoutubeAdminException(Reason reason, String internalDetail) {
        super(internalDetail);
        this.reason = reason;
    }

    public YoutubeAdminException(Reason reason, String internalDetail, Throwable cause) {
        super(internalDetail, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
