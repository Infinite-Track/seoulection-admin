# seoulection-admin

Thymeleaf 기반 관리자 서버입니다. YouTube 채널 링크와 영상 링크를 서로 다른
관리 화면에서 등록합니다.

- 채널 링크: `youtubers` 컬렉션 (Mongo)
- 영상 링크: `videos` 컬렉션, 최초 상태 `PENDING` (Mongo)
- 제품명·브랜드·카테고리: `products` 컬렉션, 최초 상태 `PENDING` (Mongo)
- **설문 문항·선택지: `survey_question` / `survey_option` 테이블 (Postgres)**

채널 등록 시 입력한 URL을 유튜버 ID로 그대로 사용하고 채널명도 함께 저장합니다.
영상 제목·유튜버 연결처럼 URL만으로 확정할 수 없는 정보는 파이프라인 처리 후 채웁니다.

## 데이터소스가 둘입니다

설문 관리만 **Postgres**를 쓰고 나머지는 Mongo입니다. 설문 마스터를 사용자 서버(api-server)가
매 제출·조회마다 읽어야 해서, 그쪽 DB에 두는 편이 자연스럽기 때문입니다.

> ⚠️ **`survey_*` 테이블의 스키마 주인은 api-server입니다.** 어드민은 `ddl-auto: none`으로 붙는
> 소비자입니다. 테이블은 api-server가 처음 기동할 때 만들어지고 초기 선택지도 그쪽이 시드합니다.
> api-server를 한 번도 띄우지 않았다면 설문 화면이 에러를 냅니다.
>
> 같은 이유로, api-server 쪽 `SurveyOptionJpaEntity` 매핑을 바꾸면 **이 저장소의 대응 엔티티도 같이
> 고쳐야 합니다.** 어긋나도 기동은 되고 쿼리할 때 터지며, 그걸 잡아주는 테스트는 없습니다.

### 설문 관리에서 지켜지는 규칙

- **코드(`code`)는 만든 뒤 수정 불가** — 사용자 응답(`survey_response`의 jsonb 배열)이 이 문자열을 그대로
  참조합니다. 도메인 타입에서 막혀 있습니다(`withDetails`에 code 파라미터가 없음).
- **삭제는 '숨김'(soft delete)** — `active=false`. 신규 설문에서만 사라지고 기존 응답은 보존되며 되살릴 수 있습니다.
- **문항은 추가·문구 수정·숨김** — 문항 키는 못 바꿉니다. 질문의 뜻이나 척도가 바뀌면 같은 키의 문구를 고치지 말고
  **새 키로 만들고 옛 문항을 숨기세요.** 옛 답이 새 뜻으로 읽히는 걸 막기 위해서입니다. api-server는 같은 지표의
  활성 문항에 답이 없는 사용자에 한해 숨긴 문항의 답으로 계속 계산합니다.
- **지표 매핑(`feature_key` · `weight` · `role`)은 문항 카드에서 편집** — api-server V30. 지표는 `skin_score_policy`의 행만
  고를 수 있고(FK, 드롭다운), 역할은 `SCORE`(산식 포함) / `MODIFIER`(보조값만) / `FLAG`(점수 없음)입니다. 선택지에는
  `caps_feature_at`(고르면 지표 점수 상한)과 `refer_dermatologist`(진료 안내)를 붙일 수 있습니다. 지표별 최소 답변 비중은
  피부 점수 정책 화면의 `min_answered_weight`입니다.
- 편집은 **즉시** 사용자 화면에 반영됩니다(배포·캐시 무효화 불필요). 실수도 즉시 반영됩니다. 매핑 변경은 이미 제출된
  응답도 다시 읽어 계산하므로 기존 사용자의 설문 점수가 바뀝니다.
- **분석기별 사진 신뢰 지표는 "분석기 신뢰 지표" 화면** — `skin_analyzer_feature`(api-server V35). 서버는 켜진 분석기
  이름의 행만 읽어 사진 리포트 필터와 문항 조회의 `photoCovered`에 쓴다. 전부 해제하면 코드 고정 목록으로 돌아간다.
- **데모 얼굴은 "데모 얼굴" 화면** — `demo_face_profile`·`demo_face_profile_score`(api-server V37). /start 첫 화면의
  얼굴 카드와 얼굴별 지표 예시 점수(0~1, 높을수록 좋음). 이미지는 웹 정적 경로(`seoulection-web/public/demo-faces/`).
- ⚠️ **배포 순서: api-server(V30·V35·V36·V37) → admin.** 어드민이 먼저 새 컬럼·테이블을 쓰면 쿼리 시점에 터집니다.

## 로컬 실행

먼저 `seoulection-server`의 로컬 MongoDB와 Postgres를 실행합니다.
설문 테이블을 만들려면 api-server도 한 번 띄워야 합니다.

```bash
cd ../seoulection-server
export JWT_SECRET=$(openssl rand -base64 48)   # compose 파일 전체를 보간하므로 먼저 실행
docker compose -f docker-compose-local.yml up -d --wait mongo postgres
docker compose -f docker-compose-local.yml up -d api-server   # survey_* 테이블 생성 + 초기 선택지 시드
```

관리자 서버를 실행합니다.

```bash
cd ../seoulection-admin
./gradlew bootRun
```

브라우저에서 다음 관리 화면으로 접속합니다.

- 어드민 홈: <http://localhost:8081/admin>
- 채널 등록: <http://localhost:8081/admin/youtubers>
- 영상 등록: <http://localhost:8081/admin/videos>
- 제품 등록: <http://localhost:8081/admin/products>
- 설문 관리: <http://localhost:8081/admin/survey>

기본 MongoDB 주소는 `mongodb://localhost:27017/seoulection`이며, 다른 주소는
`MONGODB_URI` 환경변수로 설정할 수 있습니다.

Postgres 기본값은 `jdbc:postgresql://localhost:5433/seoulection`입니다(compose가 호스트 **5433**으로
퍼블리시합니다 — 컨테이너 내부에서는 `postgres:5432`). `SPRING_DATASOURCE_URL`·`SPRING_DATASOURCE_USERNAME`·
`SPRING_DATASOURCE_PASSWORD`로 바꿀 수 있습니다.

## Docker 실행

어드민 이미지만 빌드할 때는 다음 명령을 사용합니다.

```bash
docker build -t seoulection-admin:local .
```

로컬 전체 스택에서는 MongoDB와 어드민 서버가 같은 Compose 기본 네트워크를
사용합니다. `seoulection-server`에서 실행하면 Compose가 어드민 이미지도 빌드합니다.

```bash
cd ../seoulection-server
export JWT_SECRET=$(openssl rand -base64 48)
docker compose -f docker-compose-local.yml up -d --build mongo admin-server
```

컨테이너 내부에서는 `localhost`가 아니라 Mongo 서비스명인 `mongo`로 접속하며,
관리 화면은 <http://localhost:8081/admin>에서 확인할 수 있습니다.
