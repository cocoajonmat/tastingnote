앞으로 이 내용을 기반으로 작업해줘.
반드시 COLLABORATION.md와 FEATURES.md, LEARNING.md도 함께 읽고 시작해줘.

---

## 서비스 방향
- 개인 기록 중심 (개인 7 : 소셜 3)
- 웹 서비스
- 인증: 이메일+비밀번호 + 카카오/구글/네이버 소셜 로그인 + JWT

---

## 엔티티 설계 확정

### User
- Soft Delete 방식 (deleted_at 컬럼) ✅ 구현 완료
- 회원탈퇴:
    - 탈퇴 시 deleted_at 기록 + 모든 노트 isPublic = false
    - 30일 유예 기간 후 Hard Delete 스케줄러로 완전 삭제
    - 유예 기간 중 계정 복구 가능
    - 탈퇴 시 연관 데이터(좋아요, 태그 등) 유지 → Hard Delete 시 함께 삭제
- 소셜 로그인 유저는 password null 허용
- 소셜 로그인 첫 가입 시 닉네임 설정 페이지로 이동 + 실시간 중복 체크
- 로그인 식별자: 이메일 (username 필드 제거됨)
- 닉네임 변경: 허용, 30일 1회 제한 / 프로필 URL은 내부 ID 기반 (nicknameChangedAt 컬럼 추가 필요)
- 프로필 페이지: 미니 포트폴리오 방식
    - 공개 노트 목록
    - 많이 마신 술 카테고리 (예: "위스키 애호가")
    - 평균 별점, 총 노트 수, 즐겨 쓰는 태그 Top 5 등 통계
    - 별도 집계 테이블 없이 기존 데이터 쿼리로 구현

### Alcohol
- name(영문), nameKo(한글) 컬럼 분리
- AlcoholAlias 테이블 별도 (별칭 검색용)
    - 예: "블랙라벨", "JW Black" → "조니워커 블랙라벨"로 매칭
- 술 검색 시 name + nameKo + AlcoholAlias 통합 검색
- DB에 없는 술 → Note의 alcoholName 필드에 자유 텍스트 저장 (alcohol 필드 null)
- 데이터 전략:
    - 1단계: SQL로 초기 데이터 미리 삽입 (자주 마시는 술 위주)
    - 2단계(추후): 유저 등록 요청 → 관리자 승인 방식 추가
    - 어드민 페이지는 지금 만들지 않음
- 술 상세 페이지 도입 검토 중 (친구와 상의 필요)
    - 해당 술의 평균 별점, 다른 유저 노트 목록 등 집약
    - Vivino 방식 참고 — 술 페이지가 핵심 콘텐츠가 됨
    - Discovery 기능(Q3)과도 연결됨

### Note
- alcohol 필드 (@ManyToOne, nullable) → DB에 있는 술
- alcoholName 필드 (String, nullable) → DB에 없는 술 직접 입력
- title → 필수
- rating → 필수, 5점 만점 (1.0~5.0, 0.5단위) — DECIMAL(2,1) 타입
- taste, aroma → 하이브리드 방식 (제안 목록 + 자유 입력 둘 다 허용)
- pairing, description → 자유 텍스트
- location → 자유 텍스트, 선택
- drankAt → 선택
- isPublic → 공개/비공개 토글, PUBLISHED 상태에서만 의미 있음
- NoteStatus: DRAFT(임시저장) / PUBLISHED(발행) 구분 유지
    - DRAFT: 임시저장 상태, isPublic 토글 불가
    - PUBLISHED: 발행 상태, isPublic 토글 가능

### NoteImage
- 저장 위치: AWS S3
- 업로드 시점: 노트 저장 요청 시 이미지도 같이 서버로 보내서 S3에 업로드 후 저장
- 이미지 장수: 우선 1장으로 운영 (추후 변경 가능, 미확정)

### Like (반응)
- LikeType: LIKE, LOVE, WANT, IMPRESSED, HELPFUL
- 노트당 하나만 선택 가능

### FlavorSuggestion (새 테이블 추가)
- taste/aroma 입력 시 제안 목록용
- 공통 목록 하나 (술 카테고리별 분리 안 함)
- Note 엔티티 변경 없이 별도 테이블로 관리

### Report (신고) ✅ 구현 완료
- 신고 사유: Enum (SPAM, INAPPROPRIATE, FALSE_INFO, OTHER)
- OTHER일 때만 reasonDetail 텍스트 입력
- 처리 상태: PENDING(대기) / RESOLVED(처리 완료)
- 같은 유저가 같은 노트를 중복 신고 불가
- 관리자 수동 처리 방식 (추후 자동화 가능)

---

## 기능 확정

### 검색
- 통합 검색창 하나
- 결과를 탭으로 분리 (태그 / 노트 / 술)
- 검색 대상: 태그, 노트 제목, 노트 내용
- 구현 방식: SQL LIKE 검색으로 시작, 데이터 많아져서 느려지면 MySQL Full-Text Search로 전환 예정

### 태그
- 하이브리드 방식 (기존 태그 추천 + 없으면 직접 입력)
- 입력 중 기존 태그 자동완성으로 표시 + 사용 횟수 함께 표시 (예: #위스키 (234))
- 사용 횟수는 NoteTag 개수 실시간 카운팅 방식 (Tag 테이블 컬럼 추가 없음)
- 노트당 태그 개수 제한 없음 (인스타그램 방식)
- 맛/향(taste, aroma)은 FlavorSuggestion 관리자 목록에서 선택 (Vivino 방식)

### 피드
- 공개 노트 피드
- 정렬: 좋아요 많은 순(기본) + 최신순 선택 가능
- 페이지네이션: 커서 방식 (무한 스크롤, 실시간 피드 중복/누락 방지)
- 신고 기능: 신고 버튼만 우선 구현, DB에 기록 후 관리자 수동 처리 (추후 자동화 가능)

---

## 보류 항목 (지금 구현 안 함)
- 팔로우 / 팔로워
- 댓글
- 알림
- 이메일 인증
- 비밀번호 찾기 / 재설정
- NoteImage 개수 제한 (우선 1장, 미확정)
- 기본 프로필 이미지
- 임시저장 개수 제한 및 목록 구분 방식

---

## 패키지 구조
com.dongjin.tastingnote
├── user/entity/User.java
├── user/entity/RefreshToken.java
├── user/repository/UserRepository.java
├── user/repository/RefreshTokenRepository.java
├── user/service/UserService.java
├── user/controller/UserController.java
├── user/dto/SignUpRequest.java
├── user/dto/LoginRequest.java
├── user/dto/TokenResponse.java
├── alcohol/entity/Alcohol.java
├── alcohol/entity/AlcoholAlias.java
├── note/entity/Note.java
├── note/entity/NoteImage.java
├── note/entity/Like.java
├── tag/entity/Tag.java
├── tag/entity/NoteTag.java
├── flavor/entity/FlavorSuggestion.java (미구현)
├── report/entity/Report.java
├── report/entity/ReportReason.java
├── report/entity/ReportStatus.java
├── report/repository/ReportRepository.java
├── report/service/ReportService.java
├── report/controller/ReportController.java
├── report/dto/ReportRequest.java
├── common/response/ApiResponse.java
├── common/jwt/JwtTokenProvider.java
└── common/jwt/JwtAuthenticationFilter.java

## 공통
- 모든 엔티티는 BaseEntity 상속 (createdAt, updatedAt)
- @ManyToOne은 fetch = FetchType.LAZY
- Lombok 사용
- @NoArgsConstructor(access = AccessLevel.PROTECTED)

---

## 구현 현황

### 완료
- 엔티티 전체 (User, Alcohol, AlcoholAlias, AlcoholCategory, Note, NoteImage, Like, Tag, NoteTag)
- Note CRUD (NoteService, NoteController, DTO 3종)
- JWT 인증 기반 구조 (JwtTokenProvider, JwtAuthenticationFilter)
- User 인증 API (회원가입, 로그인, 로그아웃, 토큰 재발급)
- SecurityConfig JWT 필터 등록 및 URL 인증 정책
- 환경별 설정 분리 (application-local.yaml / application-prod.yaml)
- 브랜치 전략 도입 (main + feature/*)
- 공통 ApiResponse<T> 적용 (모든 컨트롤러)
- NoteController userId → JWT에서 추출 완료
- 신고(Report) 기능 (ReportEntity, ReportService, ReportController)
- Swagger @Tag, @Operation, @SecurityRequirement 추가 (컨트롤러 3개)
- SwaggerConfig JWT 보안 스킴 등록 (Authorize 버튼)
- Note 엔티티 rating 컬럼 버그 수정 (precision/scale → columnDefinition)
- 현재 작업 브랜치: feature/jwt-auth → main 머지 완료 (2026-04-02)
- GitHub Actions CI/CD 배포 성공 확인
- 개발 환경: 노트북 → 데스크탑 전환 완료(대부분 노트북으로 작업 후 데스크탑으로 가져올 예정)

### 미완성 (다음 순서)
> 작업 시작 전 반드시 새 브랜치 먼저 만들기: `git checkout -b feature/브랜치명`

1. FlavorSuggestion 엔티티 생성
2. AlcoholService / AlcoholController
3. TagService / TagController
4. LikeService / LikeController
5. NoteImage S3 업로드
6. 소셜 로그인 (OAuth2)

---

## 인프라
- 서버: AWS Lightsail (13.124.79.235)
- CI/CD: GitHub Actions → SSH → systemctl restart tastingnote
- Swagger: http://13.124.79.235:8080/tastingnote.swagger
- 로컬: H2 인메모리 DB (application-local.yaml)
- 서버: MySQL (application-prod.yaml, 환경변수로 주입)