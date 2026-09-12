#!/usr/bin/env bash
#
# 어드민 전용 인스턴스 배포. 지금은 사람이 손으로 실행한다(자동 파이프라인 없음).
#
# ⚠️ api-server 저장소의 deploy.sh 와 달리 git 조작도, DB 백업도 하지 않는다.
#    - git: 이 인스턴스에는 클론이 없다. compose 와 이 스크립트를 scp 로 올려 쓴다.
#    - 백업: 어드민은 상태를 갖지 않는다. DB 는 API 인스턴스에 있고 그쪽 backup.sh 가 맡는다.
#
# 사용법:
#   ./deploy/deploy.sh sha-7c29165     # 이 태그로 배포
#   ./deploy/deploy.sh latest          # main 최신 이미지로 배포 (sha-<커밋7자리> 로 풀어서 기록)
#   ./deploy/deploy.sh                 # 태그 없이 = 현재 .env 값으로 재기동만
#
# 하는 일: (latest 해석) → 이전 태그 기록 → 태그 교체 → pull → up -d --wait → 실패하면 이전 태그로 롤백
#
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")/.."
REPO_ROOT="$PWD"

COMPOSE_FILE="docker-compose-prod.yml"
ENV_FILE=".env"
HISTORY_FILE=".deploy-history"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-180}"
SERVICE="admin-server"

log()  { printf '[deploy %s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
fail() { printf '[deploy ERROR] %s\n' "$*" >&2; exit 1; }
compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

# ── 중복 실행 차단. 두 프로세스가 같은 .env 를 sed 로 고치면 값이 섞인다.
# ⚠️ root 로 돌리지 말 것 — sed -i 가 .env 를 root 소유로 바꿔 다음부터 손으로 못 고친다.
exec 9>"${REPO_ROOT}/.deploy.lock"
flock -n 9 || fail "다른 배포가 진행 중이다. 중단한다."

[ -f "$COMPOSE_FILE" ] || fail "$COMPOSE_FILE 이 없다. 저장소에서 scp 로 올렸는지 확인할 것."
[ -f "$ENV_FILE" ]     || fail "$ENV_FILE 이 없다. .env.example 을 복사해 값을 채울 것."

# ── 이전 태그를 먼저 잡는다. 🔴 이 스크립트의 존재 이유다.
#    .env 는 gitignore 라 버전 관리가 없다. sed -i 로 덮는 순간 이전 값은 어디에도 안 남고,
#    그러면 새 이미지가 깨졌을 때 "무엇으로 되돌릴지" 를 알 방법이 없다.
PREV_TAG=$(grep -E '^ADMIN_IMAGE_TAG=' "$ENV_FILE" | cut -d= -f2-)
[ -n "$PREV_TAG" ] || fail "$ENV_FILE 에서 ADMIN_IMAGE_TAG 를 찾지 못했다."

TAG="${1:-$PREV_TAG}"

# ── latest 는 받자마자 sha-<커밋7자리> 로 바꿔 쓴다. 🔴 .env 에 latest 를 적지 않는다 —
#    그 이름의 뜻이 계속 바뀌어 이력이 롤백 근거가 되지 못한다(compose 의 :? 주석 참조).
#    CI 의 metadata-action 이 이미지에 커밋 SHA 라벨을 붙이므로 그걸 읽는다. EC2 에는 git 도 gh 도 없다.
if [ "$TAG" = "latest" ]; then
  # 이미지 이름은 compose 에서 읽는다. 여기 따로 적으면 두 곳이 어긋날 수 있다.
  IMAGE_REPO=$(grep -oE 'ghcr\.io/[a-z0-9._/-]+' "$COMPOSE_FILE" | head -1)
  [ -n "$IMAGE_REPO" ] || fail "$COMPOSE_FILE 에서 이미지 이름을 찾지 못했다."

  log "latest 가 가리키는 커밋 확인"
  docker pull -q "${IMAGE_REPO}:latest" >/dev/null \
    || fail "${IMAGE_REPO}:latest pull 실패 — GHCR 로그인 상태를 확인할 것."
  REVISION=$(docker image inspect -f '{{index .Config.Labels "org.opencontainers.image.revision"}}' "${IMAGE_REPO}:latest")
  # 라벨이 없으면 docker 버전에 따라 빈 값이나 "<no value>" 가 나온다. 형식으로 거른다.
  [[ "$REVISION" =~ ^[0-9a-f]{40}$ ]] || fail "latest 이미지에서 커밋 SHA 라벨을 읽지 못했다(값: ${REVISION})."
  TAG="sha-${REVISION:0:7}"

  # ⚠️ 이름을 조립했을 뿐이다. 그 태그가 실제로 같은 이미지인지 확인한다 —
  #    CI 의 짧은 SHA 길이가 바뀌면 존재하지 않는 태그나 엉뚱한 이미지를 부르게 된다.
  docker pull -q "${IMAGE_REPO}:${TAG}" >/dev/null \
    || fail "${TAG} 가 GHCR 에 없다. CI 의 SHA 태그 형식이 바뀌었는지 확인할 것."
  [ "$(docker image inspect -f '{{.Id}}' "${IMAGE_REPO}:latest")" = "$(docker image inspect -f '{{.Id}}' "${IMAGE_REPO}:${TAG}")" ] \
    || fail "latest 와 ${TAG} 가 서로 다른 이미지다. 중단한다."
  log "latest = $TAG"
fi

if [ "$TAG" = "$PREV_TAG" ]; then
  log "태그 변화 없음($TAG) — 재기동만 한다"
else
  log "배포 $PREV_TAG → $TAG"
  # 이력은 되돌릴 근거다. 태그를 바꾸기 <b>전에</b> 남긴다.
  printf '%s\t%s -> %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$PREV_TAG" "$TAG" >> "$HISTORY_FILE"
  sed -i "s|^ADMIN_IMAGE_TAG=.*|ADMIN_IMAGE_TAG=${TAG}|" "$ENV_FILE"
  grep -qE "^ADMIN_IMAGE_TAG=${TAG}$" "$ENV_FILE" || fail "$ENV_FILE 태그 교체가 반영되지 않았다."
fi

rollback() {
  printf '[deploy ROLLBACK] %s\n' "$1" >&2
  sed -i "s|^ADMIN_IMAGE_TAG=.*|ADMIN_IMAGE_TAG=${PREV_TAG}|" "$ENV_FILE"
  printf '%s\t%s -> %s (rollback)\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$TAG" "$PREV_TAG" >> "$HISTORY_FILE"
  # 옛 이미지는 로컬 캐시에 남아 있는 게 보통이지만 prune 되었을 수 있으니 pull 을 시도한다.
  compose pull "$SERVICE" || printf '[deploy ROLLBACK] 옛 이미지 pull 실패 — 로컬 캐시에 기대한다\n' >&2
  if compose up -d --wait --wait-timeout "$WAIT_TIMEOUT" "$SERVICE"; then
    printf '[deploy ROLLBACK] %s 로 복귀 성공. 배포는 실패로 처리한다.\n' "$PREV_TAG" >&2
    exit 1
  fi
  printf '[deploy FATAL] 롤백까지 실패했다. 어드민이 내려가 있다. 수동 개입이 필요하다.\n' >&2
  printf '[deploy FATAL] 이력: %s\n' "${REPO_ROOT}/${HISTORY_FILE}" >&2
  exit 2
}

log "이미지 pull"
compose pull "$SERVICE" || rollback "pull 실패 — GHCR 에 그 태그가 없을 수 있다(CI 가 올렸는지 확인)."

# --wait 는 compose healthcheck 통과까지 기다린다. 어드민의 healthcheck 는 / 가 200 인지를 본다
# (actuator 가 없어서다) — DB 가 끊겨도 통과할 수 있으니 "기동했다" 까지만 보증한다.
log "컨테이너 기동 (healthcheck 최대 ${WAIT_TIMEOUT}초 대기)"
compose up -d --wait --wait-timeout "$WAIT_TIMEOUT" "$SERVICE" || rollback "기동 또는 healthcheck 실패."

log "배포 완료: $PREV_TAG → $TAG"
compose ps --format 'table {{.Service}}\t{{.Status}}'

# healthcheck 가 못 보는 것 하나 — DB 연결. 로그에서 확인해 눈에 띄게 남긴다.
# ⚠️ grep -q 를 파이프 뒤에 쓰지 말 것. 매치 즉시 종료해 앞의 docker logs 가 SIGPIPE 로 죽고,
#    set -o pipefail 이 그걸 파이프라인 실패로 판정한다(rc=141) → 커넥션이 있는데도 WARN 이 뜬다.
#    실측으로 확인한 오탐이다(2026-09-10). grep -c 는 EOF 까지 읽으므로 SIGPIPE 가 없다.
CONN_COUNT=$(docker logs "$(compose ps -q "$SERVICE")" 2>&1 | grep -c 'Added connection' || true)
if [ "${CONN_COUNT:-0}" -gt 0 ]; then
  log "Postgres 커넥션 확인됨 (${CONN_COUNT}건)"
else
  printf '[deploy WARN] 로그에서 Postgres 커넥션을 확인하지 못했다. DB 도달을 점검할 것.\n' >&2
fi
