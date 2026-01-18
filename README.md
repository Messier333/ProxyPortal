# ProxyPortal

**ProxyPortal**은 리버스 프록시 환경에서 운영 중인 여러 서비스들을  
하나의 화면에서 관리하고 접근할 수 있도록 만든 서버 기반 개인 포털입니다.

탭 / 카테고리 / 링크 구조를 통해 서비스를 체계적으로 정리하고,  
포털 설정을 프론트가 아닌 백엔드에서 관리하는 것을 목표로 합니다.

---

## 프로젝트 동기

Nginx Proxy Manager 등으로 여러 서비스를 셀프 호스팅하면서  
서비스 URL이 늘어나고 관리가 어려워지는 문제가 있었습니다.

ProxyPortal은 이런 문제를 해결하기 위해 만들어졌습니다.

- 여러 내부 서비스를 하나의 포털로 통합
- 탭과 카테고리를 통한 구조적인 정리
- 프론트에 하드코딩되지 않은 서버 주도 설정
- 실제로 장기간 사용할 수 있는 관리형 포털

---

## 주요 기능

- 탭 / 카테고리 / 링크 등록 및 관리
- 서버에서 관리되는 포털 설정 데이터
- 백엔드에서 주입한 JSON 기반 동적 UI 구성
- 외부 서비스 연동을 고려한 구조 (예: Nginx Proxy Manager)
- 세션 기반 인증 및 접근 제어
- 심플하고 확장 가능한 대시보드 UI

---

## 아키텍처 개요

브라우저
↓
Spring MVC Controller
↓
Service / DB / 외부 API
↓
portal-config(JSON) 주입
↓
클라이언트에서 동적 UI 생성

---



## 프론트엔드 구성

- Spring MVC + Thymeleaf 기반 서버사이드 렌더링
- Vanilla JavaScript를 사용한 동적 UI 구성
- Catppuccin 컬러 팔레트 기반 UI

> 이 프로젝트의 프론트엔드는  
> 독립적인 SPA가 아닌 서버 중심 포털 UI입니다.

---

## 백엔드 구성

- Java 17
- Spring Boot
- Spring MVC
- Thymeleaf
- Spring Data JPA
- 세션 기반 인증

---

## 도메인 구조 (개념)

- **Tab**
  - 이름
  - 정렬 순서

- **Category**
  - 이름
  - 소속 Tab
  - 정렬 순서

- **Link**
  - 이름
  - URL
  - 소속 Category
  - 정렬 순서
  - 아이콘 / 색상 등 메타데이터

---

## 실행 방법

```bash
git clone https://github.com/your-id/proxyportal.git
cd proxyportal
./gradlew bootRun
```



## Frontend

The frontend portal UI of this project is based on  
**catppuccin-startpage** by Volodymyr Pivoshenko, licensed under the MIT License.

https://github.com/pivoshenko/catppuccin-startpage
