# EnjoyTrip

관광지 탐색, 여행 계획 저장, 그룹 여행 실시간 협업, AI 여행 추천, 커뮤니티 기능을 제공하는 여행 서비스입니다.  
**Spring Boot 3 REST API 백엔드**와 **Vue 3 SPA 프론트엔드**로 구성된 풀스택 프로젝트입니다.

---

## 기술 스택

### Backend
- Java 21
- Spring Boot 3.3 (Spring Web, Spring JDBC, WebSocket)
- MySQL 8 (HikariCP)
- JSON 파일 기반 영속 저장소 (users, plans, groups, boards, notices, hotplaces, comments)
- jBCrypt (비밀번호 해시)
- Jackson (JSON 직렬화, `fileObjectMapper` / HTTP 응답 `objectMapper` 분리)
- STOMP over SockJS (그룹 여행 실시간 동기화)
- GMS API — GPT-4o mini (OpenAI 호환 엔드포인트)

### Frontend
- Vue 3.4 (Composition API, `<script setup>`)
- Vite 5
- Pinia (전역 상태 — auth, toast, groupTrip)
- Vue Router 4 (Hash 모드)
- Axios
- Bootstrap 5 + Bootstrap Icons
- Kakao Maps JavaScript SDK (지도, 지오코딩, 경로)
- @stomp/stompjs + sockjs-client (WebSocket)
- marked (AI 답변 마크다운 렌더링)

---

## 주요 기능

### 1. 회원 기능
- 회원가입 · 로그인 · 로그아웃
- 마이페이지: 이름 · 비밀번호 수정, 회원 탈퇴
- **관리자(ADMIN) 권한 분리**: `User.role` 필드, 마이페이지 관리자 패널에서 유저 role 승격/변경
- NavBar에 관리자 뱃지 표시

### 2. 관광지 검색
- 시도/구군 · 관광 유형 · 키워드 기준 전국 관광지 조회
- 검색 결과를 목록 + 카카오 지도 마커로 동시 표시
- **컨텍스트 유지 추가**: 여행 상세에서 진입 시 `?planId` / `?groupId` 전달 → 관광지 선택 시 해당 여행에 즉시 추가 (모달 생략)
- **현재 여행 장소 목록** 패널: 드래그로 순서 변경(자동 저장) · 단건 삭제
- 그룹 여행 수정 시 WebSocket 연결 유지 → 다른 멤버 추가도 실시간 반영

### 3. 개인 여행 계획
- 빈 여행 생성 → 관광지 검색에서 장소 추가
- 장소 목록 드래그 앤 드롭 순서 변경 · 단건 삭제
- 숙소 주소 입력 → 카카오 지오코더 → 좌표 확인 → 등록
- **최단 경로 추천**: 다익스트라 + 순열 완전탐색, Kakao Mobility API 구간 시간, 지도 폴리라인 표시

### 4. 그룹 여행 (실시간 협업)
- 그룹 생성 · 멤버 참가 · 탈퇴 · 추방 (그룹장)
- **6자리 영문+숫자 초대 코드** 참가 방식
- STOMP WebSocket으로 장소 추가/삭제 실시간 동기화 (PLACE_ADDED / PLACE_REMOVED 이벤트)
- 카카오 지도에 멤버 추가 장소 실시간 마커 표시
- **contentId 기준 중복 장소 추가 방지**

### 5. AI 여행 도우미
- 메시지 키워드에서 지역명 · 시군구 자동 추출 (44개 지명 매핑) → DB 조회 필터 적용
- Tool 결과(관광지 이름 · 주소 · 개요)를 프롬프트에 직접 포함 → 구체적 추천 가능
- 계절 자동 감지 + 계절별 · 지역별 맞춤 여행 팁
- AI 응답 마크다운 렌더링 (marked)
- AI가 언급한 관광지 자동 카드 표시

### 6. 커뮤니티

#### 여행정보공유 (게시판)
- 게시글 CRUD (로그인 필수)
- **댓글 기능**: 작성 · 삭제 (본인 또는 관리자), DiceBear 아바타, 게시글 작성자 뱃지
- 목록에 댓글 수 배지 표시

#### 공지사항
- **관리자만** 작성 · 수정 · 삭제
- 이전/다음 공지 네비게이션

#### 핫플레이스
- **개인 장소 저장소** (본인만 조회 · 수정 · 삭제)
- 주소 입력 → 카카오 지오코딩 → 저장된 장소를 지도 마커로 표시
- 카드 클릭 시 지도 포커스 이동
- 이미지 선택 사항 (외부 URL · 로컬 업로드 모두 지원)
- 장소명 · 메모 인라인 수정

---

## 프로젝트 구조

```
EnjoyTrip/
├── backend/
│   ├── storage/
│   │   ├── data/               # JSON 영속 데이터
│   │   │   ├── users.json
│   │   │   ├── plans.json
│   │   │   ├── groups.json
│   │   │   ├── boards.json
│   │   │   ├── notices.json
│   │   │   ├── hotplaces.json
│   │   │   └── comments.json
│   │   └── uploads/hotplaces/  # 업로드 이미지
│   └── src/main/java/com/enjoytrip/
│       ├── controller/     # REST 컨트롤러 (Auth, Plan, Group, Board, Notice, Hotplace, AI)
│       ├── ai/             # AI 모듈 (client / tool / resource / prompt / planner)
│       ├── service/        # 비즈니스 로직
│       ├── repository/     # JSON 파일 기반 저장소 (AbstractJsonRepository 상속)
│       ├── model/          # 도메인 모델
│       ├── config/         # CORS, WebSocket, AppBeansConfig, GlobalExceptionHandler
│       └── util/           # IdGenerator, DateTimeUtil, PasswordHasher, JsonUtil
├── frontend/
│   └── src/
│       ├── api/            # Axios API 모듈 (planApi, groupTripApi, boardApi …)
│       ├── stores/         # Pinia (auth, toast, groupTrip)
│       ├── composables/    # useGroupWebSocket.js
│       ├── router/
│       ├── components/     # NavBar, AppToast, AddToTripModal
│       └── views/
│           ├── HomeView.vue
│           ├── AttractionsView.vue    # 관광지 검색 + 수정 모드
│           ├── TripListView.vue       # 개인 + 그룹 여행 통합 목록
│           ├── TripDetailView.vue     # 개인 여행 상세
│           ├── AiTravelView.vue
│           ├── HotplacesView.vue
│           ├── group/
│           │   └── GroupTripDetailView.vue
│           ├── board/
│           │   ├── BoardListView.vue
│           │   ├── BoardDetailView.vue
│           │   └── BoardFormView.vue
│           ├── notice/
│           │   ├── NoticeListView.vue
│           │   ├── NoticeDetailView.vue
│           │   └── NoticeFormView.vue
│           └── auth/
│               ├── LoginView.vue
│               ├── RegisterView.vue
│               ├── MypageView.vue
│               └── FindPasswordView.vue
└── README.md
```

---

## 실행 방법

### 사전 준비
- JDK 21
- MySQL 8 실행 중 (`ssafy_trip` DB + `sidos`, `guguns`, `attractions` 테이블)
- Node.js 18+

### 1. application.properties 설정

`backend/src/main/resources/application.properties`:

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

> Kakao JS 키는 `frontend/index.html`의 Kakao Maps SDK `appkey` 파라미터에서 변경합니다.

### 2. 백엔드 실행

```bash
cd backend
./mvnw spring-boot:run
# 또는 mvnw.cmd spring-boot:run (Windows)
```

서버: `http://localhost:8080`

### 3. 프론트엔드 개발 서버

```bash
cd frontend
npm install
npm run dev
```

브라우저: `http://localhost:5173`  
`/api`, `/uploads` 요청은 Vite 프록시를 통해 `localhost:8080`으로 전달됩니다.

---

## 핵심 구현 포인트

### JSON 파일 기반 영속 저장소
`AbstractJsonRepository<T>`를 상속해 각 도메인(User, Plan, Group, Board, Notice, Hotplace, Comment)이 개별 JSON 파일에 저장됩니다. `fileObjectMapper`(어노테이션 무시 → pwHash 포함 저장)와 HTTP 응답용 `objectMapper`(WRITE_ONLY 적용 → pwHash 제외)를 분리해 보안과 데이터 완전성을 동시에 보장합니다.

### 그룹 여행 실시간 협업
STOMP over SockJS로 `/topic/group/:groupId`를 구독합니다. 장소 추가/삭제 시 백엔드가 `PLACE_ADDED` / `PLACE_REMOVED` 이벤트를 broadcast해 모든 구독 클라이언트가 즉시 업데이트됩니다. 관광정보 검색 화면에서도 WebSocket 연결을 유지해 편집 중에도 다른 멤버의 변경이 실시간 반영됩니다.

### 관광지 추가 컨텍스트 유지
여행 상세에서 관광정보 검색으로 진입 시 `?planId` / `?groupId` 쿼리 파라미터로 컨텍스트를 전달합니다. `AttractionsView`는 `currentContext`(반응형 ref)를 유지하며, 관광지 선택 시 모달 없이 해당 여행에 즉시 추가합니다. "여행 변경" 버튼으로 페이지 이동 없이 대상 여행을 전환할 수 있습니다.

### 최단 경로 추천
경유지 간 이동 시간을 Kakao Mobility API로 구한 뒤, 다익스트라로 노드 간 최단 시간을 계산하고 순열 완전탐색으로 최적 방문 순서를 결정합니다. 경유지 최대 8개 지원.

### AI 여행 도우미 파이프라인

```
AiTravelController
  → TravelPlanner        # 메시지 키워드로 실행할 Tool 결정
  → Tool × N 실행
      attractionSearchTool  # 메시지에서 지역명 자동 추출 → DB 조회
      travelTipTool         # 계절 자동 감지, 지역별 맞춤 팁
      weatherTool
  → TravelPromptBuilder  # 관광지 이름·주소·개요를 프롬프트에 직접 포함
  → TravelAiClient       # GMS API(OpenAI 호환) 호출 → 마크다운 답변 생성
```
