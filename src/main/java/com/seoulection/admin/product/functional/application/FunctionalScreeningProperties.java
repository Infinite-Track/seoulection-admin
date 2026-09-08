package com.seoulection.admin.product.functional.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code admin.functional-screening.*} 설정. 기본값은 <b>가장 보수적인 쪽</b>으로 둔다. */
@ConfigurationProperties(prefix = "admin.functional-screening")
public class FunctionalScreeningProperties {

    private boolean enabled = true;

    /**
     * 자동 판정 결과로 제품 상태까지 바로 옮길지. <b>기본은 꺼짐</b>.
     *
     * <p>자동 조회는 근거를 모아 폼을 채워 두는 데까지만 하고, 확정은 어드민이 누른다.
     * 기능성은 규제 정보라 "모델이 골랐다"와 "사람이 확인했다" 사이에 한 칸이 있어야 한다 —
     * 자동 판정이 틀렸을 때 되돌리는 비용이 한 번 더 클릭하는 비용보다 훨씬 크다.
     *
     * <p>켜면 판정이 끝나는 즉시 상태가 전진한다. 일치율을 충분히 확인한 뒤에 켤 스위치다.
     */
    private boolean applyDecisions = false;

    /** 이 점수 이상이면 LLM 판정 없이도 확정 후보가 된다(숫자·업체·유형 규칙은 그대로 통과해야 한다). */
    private double autoThreshold = 0.95;

    /** 이 점수 미만은 후보로도 남기지 않는다. 표를 후보로 채워 놓으면 검수가 더 느려진다. */
    private double candidateThreshold = 0.60;

    /**
     * 우리가 적은 이름이 등록명에 이 비율 이상 담겨 있으면, 유사도가 낮아도 후보로 남긴다.
     * 제품명을 일부만 입력한 경우를 건지기 위한 값이다.
     */
    private double coverageThreshold = 0.95;

    /** 화면에 남길 후보 수. 이름을 일부만 적으면 같은 계열이 여럿 걸려 넉넉해야 한다. */
    private int maxCandidates = 8;

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
    public double getCoverageThreshold() { return coverageThreshold; }
    public void setCoverageThreshold(double value) { this.coverageThreshold = value; }
    public int getMaxCandidates() { return maxCandidates; }
    public void setMaxCandidates(int maxCandidates) { this.maxCandidates = maxCandidates; }
    public boolean isAutoConcludeNone() { return autoConcludeNone; }
    public void setAutoConcludeNone(boolean autoConcludeNone) { this.autoConcludeNone = autoConcludeNone; }
    public Mfds getMfds() { return mfds; }
    public Llm getLlm() { return llm; }

    public static class Mfds {
        /**
         * data.go.kr 발급 키. 심사·보고 두 서비스가 같은 키를 쓴다.
         *
         * <p>URL 인코딩된 형태 그대로 넣는다({@code ...%2FXm1Lv...%3D%3D}).
         * 비어 있으면 조회가 전부 FAILED로 남는다.
         */
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

        /** Gemini 모델. 이름 표기 변환·후보 판정은 무거운 추론이 아니라 flash로 충분하다. */
        private String model = "gemini-3.8-flash";

        /** Google AI Studio 발급 키. 비어 있으면 LLM 호출을 건너뛰고 규칙만으로 판정한다. */
        private String apiKey = "";

        /** Gemini Interactions API 엔드포인트. */
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/interactions";

        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 20000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int value) { this.connectTimeoutMs = value; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int value) { this.readTimeoutMs = value; }
    }
}
