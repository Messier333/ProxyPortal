# ProxyPortal

ProxyPortal은 리버스 프록시 환경에서 운영하는 여러 서비스를 한 화면에서 접근하고 관리하기 위한 서버 사이드 포털입니다.  
탭/카테고리/링크 구조로 서비스를 정리하고, 계정 및 권한 기반으로 접근을 제어합니다.

## 핵심 기능

- 탭/카테고리/링크 CRUD 및 정렬
- 관리자 계정 기반 운영 관리
- Spring Security 인증(세션 + remember-me)
- 로그인 시도 제한 및 차단
- PostgreSQL + Flyway 기반 스키마 마이그레이션
- 업로드 파일 영속화(`/app/uploads`)

## 기술 스택

- Java 21
- Spring Boot 3
- Spring MVC + Thymeleaf
- Spring Data JPA
- Spring Security
- Spring Session JDBC
- Flyway
- PostgreSQL

## 로컬 실행

```bash
git clone https://github.com/Messier333/ProxyPortal.git
cd ProxyPortal
./gradlew bootRun
```

## 배포 방식

이 저장소는 GHCR 이미지를 사용한 Docker 배포를 기준으로 구성되어 있습니다.

- 이미지: `ghcr.io/messier333/proxyportal`
- 워크플로: `.github/workflows/docker-ghcr.yml`
- 트리거: `dev -> main` PR 머지 시 이미지 빌드/푸시

## Docker Compose 실행

아래 내용을 `compose.yml`로 넣고 `<...>` 값을 실제 값으로 바꿔 사용하면 됩니다.

```yaml
services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: proxyportal
      POSTGRES_USER: proxyportal
      POSTGRES_PASSWORD: "<postgres password>"
      TZ: Asia/Seoul
    volumes:
      - "<postgresql data path>:/var/lib/postgresql/data"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 10

  app:
    image: ${APP_IMAGE:-ghcr.io/messier333/proxyportal:latest}
    restart: unless-stopped
    pull_policy: always
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      PUID: 568
      PGID: 568
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/proxyportal
      SPRING_DATASOURCE_USERNAME: proxyportal
      SPRING_DATASOURCE_PASSWORD: "<postgres password>"
      REMEMBER_ME_KEY: "<random secret for signing remember-me cookies, e.g. 64+ chars>"
      NPM_BASE_URL: "<npm url>"
      NPM_IDENTITY: "<npm id>"
      NPM_SECRET: "<npm password>"
      TZ: Asia/Seoul
    ports:
      - "<proxyportal port>:8080"
    volumes:
      - "<image path>:/app/uploads"

volumes:
  proxyportal_postgres_data:
  proxyportal_uploads:
```

실행:

```bash
docker compose pull
docker compose up -d
docker compose logs -f app
```

## 주요 환경변수

| 변수 | 설명 |
|---|---|
| `POSTGRES_DB` | PostgreSQL DB 이름 |
| `POSTGRES_USER` | PostgreSQL 계정 |
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 |
| `REMEMBER_ME_KEY` | remember-me 쿠키 서명용 비밀키 (충분히 긴 랜덤값 권장) |
| `NPM_BASE_URL` | Nginx Proxy Manager API base URL |
| `NPM_IDENTITY` | NPM API 계정 |
| `NPM_SECRET` | NPM API 비밀번호/시크릿 |
| `PUID` | 앱 컨테이너 실행 UID |
| `PGID` | 앱 컨테이너 실행 GID |

## 운영 메모

- 컨테이너 이미지는 기본적으로 `SPRING_PROFILES_ACTIVE=prod`로 실행됩니다.
- 빈 PostgreSQL에 처음 붙으면 Flyway가 `db/migration` SQL을 자동 적용합니다.
- 데이터는 named volume에 유지됩니다.
  - `proxyportal_postgres_data`
  - `proxyportal_uploads`

## Acknowledgement

Frontend UI는 아래 프로젝트를 기반으로 구성되었습니다.

- https://github.com/pivoshenko/catppuccin-startpage
