# EnjoyTrip

관광지 탐색, 여행 계획 저장, 숙소 기준 최적 경로 추천, 커뮤니티 기능을 제공하는 여행 서비스입니다.  
**Spring Boot 3 REST API 백엔드**와 **Vue 3 SPA 프론트엔드**로 구성된 풀스택 프로젝트입니다.

---

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.3.5 (Spring Web, Spring JDBC)
- MySQL 8 (HikariCP 커넥션 풀)
- jBCrypt (비밀번호 해시)
- Jackson (JSON 직렬화)
- springdoc-openapi 2.6 (Swagger UI)

### Frontend
- Vue 3.4 (Composition API, `<script setup>`)
- Vite 5
- Pinia (전역 상태 관리)
- Vue Router 4 (Hash 모드)
- Axios
- Bootstrap 5 + Bootstrap Icons
- Kakao Maps JavaScript SDK

---

## 주요 기능

### 1. 회원 기능
- 회원가입, 로그인, 로그아웃
- 마이페이지에서 이름 및 비밀번호 수정
- 현재 비밀번호 확인 후 회원 탈퇴

### 2. 관광지 검색
- 지역(시도/구군), 관광 유형, 키워드 기준으로 관광지 조회
- 검색 결과를 목록과 카카오 지도에 함께 표시
- 관광지를 여행 계획 드래프트에 즉시 추가 가능

### 3. 여행 계획 관리
- 드래프트에 장소 추가·삭제·순서 변경
- 숙소 정보를 포함해 계획 저장
- 내 계획 목록 조회 및 삭제

### 4. 최적 경로 추천
- 저장된 여행 계획을 기반으로 방문 순서 최적화
- 숙소 출발 → 전체 경유지 방문 → 숙소 복귀 왕복 경로
- Kakao Mobility API + 다익스트라 + 순열 완전탐색 조합
- 총 소요시간·이동거리, 구간별 정보, 지도 폴리라인 제공

### 5. 커뮤니티
- 자유 게시판 CRUD
- 공지사항 CRUD (관리자)
- 핫플레이스 이미지 업로드 및 목록·삭제

### 6. Spring AI 특화 모듈
- `client`, `tool`, `resource`, `prompt`, `planner` 역할을 분리한 AI 응답 구조
- 기존 관광지 검색 서비스를 `attractionSearchTool` 단일 Tool로 호출
- Planner가 요청 의도에 따라 관광지, 날씨, 여행 팁 Tool을 연쇄 호출
- Tool 실행 결과(Resource)를 Prompt에 반영하고 서버 주도 응답 생성

---

## 프로젝트 구조

```
EnjoyTrip/                          # 모노레포 루트
├── backend/                        # Spring Boot REST API 서버
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   ├── storage/                    # 런타임 데이터 (gitignore: uploads/*)
│   │   ├── data/                   # JSON 영속 데이터
│   │   └── uploads/                # 업로드 파일
│   └── src/main/java/com/enjoytrip
│       ├── controller      # REST API 컨트롤러
│       ├── ai              # Spring AI 특화 모듈 (client/tool/resource/prompt/planner)
│       ├── service         # 비즈니스 로직
│       ├── repository      # DB / 파일 데이터 접근
│       ├── model           # 도메인 모델
│       ├── config          # CORS, 설정 빈
│       └── util            # 유틸리티
├── frontend/                       # Vue 3 SPA
│   ├── index.html
│   ├── vite.config.js
│   └── src/
│       ├── api/            # axios API 모듈
│       ├── stores/         # Pinia 스토어 (auth, toast)
│       ├── router/         # Vue Router 라우트 정의
│       ├── views/          # 페이지 컴포넌트
│       │   ├── HomeView.vue
│       │   ├── AttractionsView.vue
│       │   ├── PlansView.vue
│       │   ├── HotplacesView.vue
│       │   ├── auth/       # 로그인, 회원가입, 마이페이지, 비밀번호 찾기
│       │   ├── board/      # 게시판 목록, 상세, 작성/수정
│       │   └── notice/     # 공지사항 목록, 상세, 작성/수정
│       └── components/     # NavBar, AppToast
└── docs/
    └── imgs/               # 문서용 이미지
```

---

## 실행 방법

### 사전 준비

- JDK 21
- MySQL 8 실행 중
- `ssafy_trip` 데이터베이스 및 `sidos`, `guguns`, `attractions` 테이블 준비
- Node.js 18+

### 1. application.properties 설정

`backend/src/main/resources/application.properties`에서 DB 정보와 API 키를 확인합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ssafy_trip?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=ssafy
spring.datasource.password=ssafy

kakao.mobility.rest.key=YOUR_KAKAO_MOBILITY_REST_KEY

app.storage.dir=./storage
app.route.max-waypoints=8
```

> Kakao JS 키는 `frontend/index.html`의 Kakao Maps SDK `appkey` 파라미터에서 직접 변경합니다.

### 2. 백엔드 실행

```bash
cd backend
./mvnw spring-boot:run
```

서버가 `http://localhost:8080`으로 시작됩니다.  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 3. 프론트엔드 개발 서버 실행

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`으로 접속합니다.  
`/api`, `/uploads` 요청은 Vite 프록시를 통해 `localhost:8080`으로 전달됩니다.

### 4. 프론트엔드 빌드 (운영 배포용)

```bash
cd frontend
npm run build
```

빌드 결과물은 `frontend/dist/`에 생성됩니다.

---

## API 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/sidos` | 시도 목록 조회 |
| `GET` | `/api/guguns?sidoCode=` | 구군 목록 조회 |
| `GET` | `/api/attractions` | 관광지 검색 |
| `POST` | `/api/auth/login` | 로그인 |
| `POST` | `/api/auth/logout` | 로그아웃 |
| `GET` | `/api/auth/me` | 현재 로그인 사용자 조회 |
| `GET/POST` | `/api/plans` | 여행 계획 목록 조회 / 저장 |
| `DELETE` | `/api/plans/{id}` | 여행 계획 삭제 |
| `GET` | `/api/plans/{id}/route` | 최적 경로 추천 |
| `GET/POST/PUT/DELETE` | `/api/boards/{id}` | 게시판 CRUD |
| `GET/POST/PUT/DELETE` | `/api/notices/{id}` | 공지사항 CRUD |
| `GET/POST/DELETE` | `/api/hotplaces/{id}` | 핫플레이스 CRUD |
| `GET` | `/api/ai/capabilities` | 등록된 AI Tool 목록 |
| `POST` | `/api/ai/tools?name=` | 단일 Tool 호출 |
| `POST` | `/api/ai/chat` | Planner 기반 다중 Tool 호출 및 AI 응답 |

---

## 화면 라우트

| 경로 | 설명 |
|------|------|
| `/#/` | 홈 |
| `/#/login` | 로그인 |
| `/#/register` | 회원가입 |
| `/#/mypage` | 마이페이지 |
| `/#/attractions` | 관광지 검색 |
| `/#/plans` | 여행 계획 |
| `/#/ai` | AI 여행도우미 |
| `/#/boards` | 자유 게시판 |
| `/#/notices` | 공지사항 |
| `/#/hotplaces` | 핫플레이스 |

---

## 핵심 구현 포인트

### bcrypt 비밀번호 해시
로그인, 정보 수정, 회원 탈퇴 모두 jBCrypt 해시 비교 기반으로 검증합니다.

### 다익스트라 + 순열 완전탐색 경로 추천
경유지 간 이동 시간을 Kakao Mobility API로 구한 뒤, 다익스트라로 노드 간 최단 시간을 계산하고 순열 완전탐색으로 최적 방문 순서를 결정합니다. 경유지 최대 8개를 지원합니다.

### Vue 3 SPA + Spring Boot REST 분리 구조
기존 JSP 서버사이드 렌더링에서 REST API 백엔드 / Vue SPA 프론트엔드 구조로 전환했습니다. Vite 개발 서버 프록시로 CORS 없이 로컬 개발이 가능하며, 운영 시에는 Spring Boot CorsFilter로 허용 오리진을 제어합니다.

### Spring AI 특화 모듈 설계
PDF 요구사항의 필수 기능(F1201~F1205)에 맞춰 사용자 요청을 `AiTravelController`가 수신하고, `TravelPlanner`가 필요한 Tool 순서를 결정합니다. 각 Tool은 하나의 책임만 가지며 기존 Service 또는 로컬 Resource를 조회합니다. `TravelPromptBuilder`는 Tool 결과만 Prompt에 포함하고, `TravelAiClient`는 검증된 Resource 기반으로 최종 응답을 생성합니다.

심화 기능(F1206~F1209)은 Planner 구조와 README 고찰로 정리했습니다. 단일 Tool 호출은 빠르고 검증이 단순하지만 복합 질문 처리 범위가 좁고, 다중 Tool 호출은 관광지·날씨·팁처럼 서로 다른 Resource를 결합할 수 있는 대신 호출 순서와 실패 처리가 중요합니다. 향후 실제 Spring AI `ChatClient` 또는 별도 AI Server로 분리할 경우 현재 `TravelAiClient` 구현체만 교체하고 Tool/Planner 계층은 유지할 수 있습니다.
