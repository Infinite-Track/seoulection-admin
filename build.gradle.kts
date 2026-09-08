plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.seoulection"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
	// Claude API — 기능성 스크리닝의 이름 해석(브랜드 한글 표기·등록명 후보·동일 제품 판정)에 쓴다.
	// ⚠️ 이 의존성이 없으면 ClaudeProductNameResolver 가 컴파일되지 않아 앱이 아예 기동하지 않는다.
	//    LLM 을 끄는 것은 admin.functional-screening.llm.enabled=false 로 하는 것이지,
	//    의존성을 빼는 것이 아니다(@ConditionalOnProperty 는 런타임 스위치다).
	implementation("com.anthropic:anthropic-java:2.34.0")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    // 설문 문항·선택지와 성분 카탈로그는 Postgres다(나머지 관리 대상은 Mongo). 스키마 주인은 api-server이고
    // 여기서는 ddl-auto=none으로 붙는다 — application.yml 주석 참조.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-mongodb-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Docker 빌드에서 "의존성 내려받기"를 소스 복사보다 먼저 끝내기 위한 태스크(Dockerfile 2단계).
// 이것만으로 build.gradle.kts가 안 바뀌는 한 의존성 레이어가 통째로 캐시된다.
// ⚠️ `gradlew dependencies`를 쓰면 안 된다 — 그건 의존성 "그래프"만 렌더링해서 POM 메타데이터만 받고,
//    정작 덩치 큰 jar는 안 받는다. 파일을 실제로 받으려면 configuration을 resolve해야 한다.
// bootJar에 필요한 것만 담는다(test 계열 제외) — Testcontainers·JUnit은 런타임 이미지에 불필요하다.
tasks.register("resolveDependencies") {
    doLast {
        listOf("compileClasspath", "runtimeClasspath", "annotationProcessor")
            .forEach { configurations.getByName(it).resolve() }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
