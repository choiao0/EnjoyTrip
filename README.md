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
- GMS API — GPT-4o mini (OpenAI 호환 엔드포인트, `gms.model`로 모델 전환 가능)

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

### 6. AI 여행 도우미
- `client`, `tool`, `resource`, `prompt`, `planner` 역할을 분리한 AI 응답 구조
- 기존 관광지 검색 서비스를 `attractionSearchTool` 단일 Tool로 호출
- Planner가 요청 의도에 따라 관광지, 날씨, 여행 팁 Tool을 연쇄 호출
- Tool 실행 결과(Resource)를 Prompt에 반영하고 `TravelAiClient`가 GMS API(Claude Haiku)를 호출해 최종 답변 생성

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

gms.api.url=https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions
gms.api.key=YOUR_GMS_KEY
gms.model=gpt-4o-mini

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

### AI 여행 도우미 설계

사용자 요청을 `AiTravelController`가 수신하면 아래 순서로 처리합니다.

```
AiTravelController
  → TravelPlanner       # 메시지 키워드로 실행할 Tool 목록 결정
  → Tool × N 실행       # attractionSearchTool(DB), weatherTool, travelTipTool
  → TravelPromptBuilder # Tool 결과(Resource)를 프롬프트 문자열로 조립
  → TravelAiClient      # GMS API 호출 → Claude Haiku가 최종 답변 생성
```

각 Tool은 단일 책임을 가집니다. `attractionSearchTool`은 기존 `AttractionService`를 그대로 재사용하고, `weatherTool`과 `travelTipTool`은 로컬 데이터를 반환합니다. `TravelPromptBuilder`는 Tool 결과만 Prompt에 포함해 LLM이 주어진 Resource 밖을 추측하지 않도록 제약합니다.

`TravelAiClient`는 GMS OpenAI 호환 엔드포인트(`/v1/chat/completions`)를 Spring `RestClient`로 호출합니다. 모델·URL·키는 `application.properties`로 외부화되어 있어 `gms.model` 값만 바꾸면 Claude / GPT / Gemini 간 전환이 가능합니다.

단일 Tool 호출(`POST /api/ai/tools?name=`)은 빠르고 검증이 단순하며, 다중 Tool 호출(`POST /api/ai/chat`)은 관광지·날씨·팁을 결합한 복합 답변을 생성합니다.
