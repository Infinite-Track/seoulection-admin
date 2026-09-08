package com.seoulection.admin.product.functional.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code admin.functional-screening.*} 설정. 기본값은 <b>가장 보수적인 쪽</b>으로 둔다. */
@ConfigurationProperties(prefix = "admin.functional-screening")
public class FunctionalScreeningProperties {

    private boolean enabled = true;

    /**
     * 자동 판정 결과로 제품 상태를 실제로 옮길지. 기본은 켜짐 — 한글 이름을 저장하면 그 자리에서
     * 기능성까지 확정되고 어드민은 <b>자동이 못 찾은 것만</b> 손댄다.
     *
     * <p>끄면 그림자 모드가 된다(판정은 기록하되 상태는 그대로). 판정 규칙을 크게 손본 뒤
     * 사람 판단과 대조해 보고 싶을 때 쓰라고 남겨 둔 스위치다.
     */
    private boolean applyDecisions = true;

    /** 이 점수 이상이면 LLM 판정 없이도 확정 후보가 된다(숫자·업체·유형 규칙은 그대로 통과해야 한다). */
    private double autoThreshold = 0.95;

    /** 이 점수 미만은 후보로도 남기지 않는다. 표를 후보로 채워 놓으면 검수가 더 느려진다. */
    private double candidateThreshold = 0.60;

    /** 화면에 남길 후보 수. */
    private int maxCandidates = 5;

    /**
     * "기능성 아님"까지 자동 확정할지. 기본은 꺼짐.
     *
     * <p>검색 실패와 기능성 아님은 겉보기가 같다(둘 다 후보 0건). 브랜드 전수 조회가 0건일
     * 때만 둘을 가를 수 있고, 그마저도 선크림 같은 제품엔 적용하지 않는다.
     */
    private boolean autoConcludeNone = false;

    private final Mfds mfds = new Mfds();
    private final Llm llm = new Llm();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isApplyDecisions() { return applyDecisions; }
    public void setApplyDecisions(boolean applyDecisions) { this.applyDecisions = applyDecisions; }
    public double getAutoThreshold() { return autoThreshold; }
    public void setAutoThreshold(double autoThreshold) { this.autoThreshold = autoThreshold; }
    public double getCandidateThreshold() { return candidateThreshold; }
    public void setCandidateThreshold(double candidateThreshold) { this.candidateThreshold = candidateThreshold; }
    public int getMaxCandidates() { return maxCandidates; }
    public void setMaxCandidates(int maxCandidates) { this.maxCandidates = maxCandidates; }
    public boolean isAutoConcludeNone() { return autoConcludeNone; }
    public void setAutoConcludeNone(boolean autoConcludeNone) { this.autoConcludeNone = autoConcludeNone; }
    public Mfds getMfds() { return mfds; }
    public Llm getLlm() { return llm; }

    public static class Mfds {
        /** data.go.kr 발급 키(URL 인코딩된 값 그대로). 비어 있으면 조회가 전부 FAILED로 남는다. */
        private String serviceKey = "";
        private String reportUrl = "https://apis.data.go.kr/1471000/FtnltCosmRptPrdlstInfoService/getRptPrdlstInq";
        private String examUrl = "https://apis.data.go.kr/1471057/FtnltCosmSrngPrdlstInfoService04/getSrngPrdlstInq";
        private int pageSize = 500;
        /** 브랜드 전수 조회 페이지 상한. 큰 브랜드(메디큐브 226건)를 담되 무한 페이징은 막는다. */
        private int maxBrandPages = 4;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 15000;

        public String getServiceKey() { return serviceKey; }
        public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
        public String getReportUrl() { return reportUrl; }
        public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }
        public String getExamUrl() { return examUrl; }
        public void setExamUrl(String examUrl) { this.examUrl = examUrl; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public int getMaxBrandPages() { return maxBrandPages; }
        public void setMaxBrandPages(int maxBrandPages) { this.maxBrandPages = maxBrandPages; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public static class Llm {
        private boolean enabled = false;
        private String model = "claude-opus-5";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}
