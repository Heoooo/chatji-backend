<div align="center">
  <h1>🎯 Chatji (찾지)</h1>
  <p><strong>실시간 맞춤형 핫딜 스나이핑 & 알림 서비스</strong></p>
  <p><i>"당신이 잠든 사이에도 최저가를 찾습니다."</i></p>
  <br/>
  
  [![Java](https://img.shields.io/badge/Java-17+-007396?style=flat-square&logo=java)](#)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=spring)](#)
  [![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)](#)
  [![Redis](https://img.shields.io/badge/Redis-Latest-DC382D?style=flat-square&logo=redis)](#)
  [![Vue/React](https://img.shields.io/badge/Frontend-React%20%7C%20Vite-61DAFB?style=flat-square&logo=react)](#)
</div>

<br/>

##  프로젝트 개요
기존 쇼핑 검색 서비스(네이버 쇼핑 등)는 사용자가 직접 검색해야 하는 '수동적' 구조이며, 실제 가격 변동폭이 작아 '진짜 할인'을 체감하기 어렵다는 한계가 있었습니다. 이를 해결하기 위해 여러 커뮤니티의 핫딜 정보를 실시간으로 수집하고 개인화된 조건(키워드, 카테고리, 목표가)에 맞춰 능동적으로 사용자에게 알려주는 **지능형 핫딜 스나이핑 서비스**를 기획 및 개발했습니다.

단순한 데이터 수집을 넘어, 네이버 쇼핑 최저가와 실시간으로 교차 검증하여 진짜 핫딜(할인율 검증)만을 필터링해 제공하는 스마트 엔진이 핵심입니다.

---

##  주요 기능 (Key Features)

- **지능형 알림(낚싯대) 설정**: 키워드(예: 맥북), 카테고리(예: 가전 > 노트북), 목표가(예: 1,500,000원 이하) 등 세밀한 조건 설정 가능
- **실시간 핫딜 매칭 및 크롤링 엔진**: 대형 커뮤니티(뽐뿌, 루리웹 등) 핫딜 게시판을 정기 크롤링 및 정규표현식(Regex)을 통한 데이터 정제
- **SSE 기반 실시간 푸시 알림**: Server-Sent Events(SSE)를 활용해 매칭된 핫딜 정보를 사용자의 브라우저로 지연 없이 즉시 발송
- **핫딜 스코어링 및 시세 분석**: 자체 수집된 핫딜가와 네이버 API 최저가를 대조해 할인율 계산 및 신뢰도 스코어 부여

---

##  기술적 도전 및 성능 최적화 (Technical Highlights)
**백엔드 개발자로서 성능 병목을 해결하고 대규모 트래픽/데이터 환경에 대비한 아키텍처를 설계했습니다.** 상세한 분석 및 측정 결과는 [최적화 리포트(PERFORMANCE_OPTIMIZATION.md)](./PERFORMANCE_OPTIMIZATION.md)에서 확인하실 수 있습니다.

### 1.  알림 매칭 엔진 최적화 (O(N) → O(Log N))
*   **문제**: 수집된 핫딜을 전체 유저의 알림 설정과 매칭할 때, 애플리케이션 메모리 상에서 Loop를 도는 방식으로 인해 유저 수 증가 시(1만 건 기준) 심각한 응답 지연(742ms) 발생
*   **해결**: 
    *   Java Loop 비교 연산을 **JPQL 기반 최적화된 DB 검색 쿼리**로 이관
    *   `Alert` 테이블의 `keyword`, `category` 컬럼에 **복합 인덱스(Composite Index)** 설계
*   **결과**: 1만 건 기준 매칭 소요 시간 **742.34ms → 12.18ms (약 98% 단축)** 달성

### 2.  외부 API 장애 격리 및 시스템 보호 (Circuit Breaker)
*   **문제**: 네이버 쇼핑 API의 Rate Limit 초과나 일시적 장애가 전체 서비스 마비(Cascading Failure)로 이어질 위험 존재
*   **해결**: **Resilience4j Circuit Breaker** 패턴 도입. 실패율 임계치 도달 시 즉시 회로를 차단(Open)하여 Fail-fast를 유도하고 Fallback 처리
*   **결과**: 외부 API 장애 시에도 서버 자원(Thread) 고갈을 방지하고 가용성 100% 유지

### 3.  비동기 병렬 처리 엔진 구축 (Async Parallel)
*   **문제**: 다중 페이지 핫딜 데이터를 수집할 때 순차적 API 호출로 인한 I/O Bound 병목 현상 발생
*   **해결**: `CompletableFuture`와 커스텀 `ThreadPoolTaskExecutor`를 활용해 API 요청 병렬 처리
*   **결과**: 상품 검색 API 평균 응답 시간 **1,604ms → 565ms (약 65% 단축)**

### 4.  하이엔드 캐싱 전략 (Multi-level TTL & Cache Stampede 방지)
*   **문제**: 빈번한 API 호출 비용 절감 및 빠른 응답 필요. 반면 가격 정보의 최신성도 유지해야 함
*   **해결**: 
    *   데이터 성격에 따라 검색 결과(10분 TTL)와 가격 정보(3분 TTL) 다중 캐싱 정책 수립
    *   Spring Cache의 `sync=true` 옵션을 적용해 **Cache Stampede(캐시 만료 시 동시 다발적 요청)** 현상 방지

---

## 🏗 시스템 아키텍처 및 폴더 구조 (Architecture)

```text
chatji-backend/
├── src/main/java/com/chatji/chatji
│   ├── client/       # 외부 연동 (Naver API Client 등)
│   ├── config/       # Spring, Security, Async, Cache 설정
│   ├── controller/   # REST API 엔드포인트
│   ├── domain/       # JPA Entity & Repository (Alert, HotDeal, PriceHistory 등)
│   ├── exception/    # 글로벌 예외 처리
│   └── service/      # 비즈니스 로직 (AlertService, CrawlerService, ProductService 등)
└── docker-compose.yml # 인프라 구성 (Redis, MySQL)
```

---

## ⚙️ 시작 가이드 (Getting Started)

**1. 환경 변수 설정 (application-secret.yml 생성)**
```yaml
naver:
  openapi:
    client-id: "본인의 클라이언트 ID"
    client-secret: "본인의 시크릿"
```

**2. 의존성 설치 및 빌드**
```bash
./gradlew clean build
```

**3. 애플리케이션 실행**
```bash
# 로컬 환경 (H2 In-Memory DB & Caffeine Cache 사용)
java -jar build/libs/chatji-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```
*(운영 환경의 경우 `docker-compose up -d`를 통해 Redis 및 DB 세팅 후 `prod` 프로필로 실행합니다.)*

---

> **Note to Interviewers:** 
> 본 레포지토리는 포트폴리오 목적으로 관리되며, 대용량 트래픽 환경에서의 병목 해결과 데이터 정합성을 고민한 흔적이 담겨있습니다. 코드와 아키텍처에 관한 피드백은 언제든 환영합니다! 🙇‍♂️
