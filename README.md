# seoulection-admin

Thymeleaf 기반 관리자 서버입니다. YouTube 채널 링크와 영상 링크를 서로 다른
관리 화면에서 등록합니다.

- 채널 링크: `youtubers` 컬렉션
- 영상 링크: `videos` 컬렉션, 최초 상태 `PENDING`
- 제품명·브랜드·카테고리: `products` 컬렉션, 최초 상태 `PENDING`

채널 등록 시 입력한 URL을 유튜버 ID로 그대로 사용하고 채널명도 함께 저장합니다.
영상 제목·유튜버 연결처럼 URL만으로 확정할 수 없는 정보는 파이프라인 처리 후 채웁니다.

## 로컬 실행

먼저 `seoulection-server`의 로컬 MongoDB를 실행합니다.

```bash
cd ../seoulection-server
docker compose -f docker-compose-local.yml up -d mongo
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

기본 MongoDB 주소는 `mongodb://localhost:27017/seoulection`이며, 다른 주소는
`MONGODB_URI` 환경변수로 설정할 수 있습니다.

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
