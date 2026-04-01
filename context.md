앞으로 이 내용을 기반으로 작업해줘.

---

## 서비스 방향
- 개인 기록 중심 (개인 7 : 소셜 3)
- 웹 서비스
- 인증: 이메일+비밀번호 + 카카오/구글/네이버 소셜 로그인 + JWT

---

## 엔티티 설계 확정

### User
- Soft Delete 방식 (deleted_at 컬럼)
- 탈퇴 시 → deleted_at에 현재시간 저장 + 해당 유저의 모든 Note.isPublic = false
- 추후 선택적으로 Hard Delete 스케줄러 추가
- 소셜 로그인 유저는 password null 허용
- 소셜 로그인 첫 가입 시 닉네임 설정 페이지로 이동 + 실시간 중복 체크

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

### Note
- alcohol 필드 (@ManyToOne, nullable) → DB에 있는 술
- alcoholName 필드 (String, nullable) → DB에 없는 술 직접 입력
- title → 필수
- rating → 필수, 5점 만점 (1~5 정수), 추후 변경 가능성 있음
- taste, aroma → 하이브리드 방식 (제안 목록 + 자유 입력 둘 다 허용)
- pairing, description → 자유 텍스트
- location → 자유 텍스트, 선택
- drankAt → 선택
- isPublic → 공개/비공개 토글, PUBLISHED 상태에서만 의미 있음
- NoteStatus: DRAFT(임시저장) / PUBLISHED(발행) 구분 유지
    - DRAFT: 임시저장 상태, isPublic 토글 불가
    - PUBLISHED: 발행 상태, isPublic 토글 가능

### Like (반응)
- LikeType: LIKE, LOVE, WANT, IMPRESSED, HELPFUL
- 노트당 하나만 선택 가능

### FlavorSuggestion (새 테이블 추가)
- taste/aroma 입력 시 제안 목록용
- 공통 목록 하나 (술 카테고리별 분리 안 함)
- Note 엔티티 변경 없이 별도 테이블로 관리

---

## 기능 확정

### 검색
- 통합 검색창 하나
- 결과를 탭으로 분리 (태그 / 노트 / 술)
- 검색 대상: 태그, 노트 제목, 노트 내용

### 태그
- 하이브리드 방식 (기존 태그 추천 + 없으면 직접 입력)
- 입력 중 기존 태그 자동완성으로 표시
- 노트당 태그 개수 제한 없음 (인스타그램 방식)

### 피드
- 공개 노트 피드
- 정렬: 좋아요 많은 순(기본) + 최신순 선택 가능

---

## 보류 항목 (지금 구현 안 함)
- 팔로우 / 팔로워
- 댓글
- 알림
- 이메일 인증
- 비밀번호 찾기 / 재설정
- NoteImage 개수 제한
- 피드 페이지네이션 방식
- 로그인 식별자 (이메일 vs username)
- 기본 프로필 이미지
- JWT 인증 연동 (엔티티/기능 설계 마무리 후 추가)
- 임시저장 개수 제한 및 목록 구분 방식

---

## 패키지 구조
com.dongjin.tastingnote
├── user/entity/User.java
├── alcohol/entity/Alcohol.java
├── alcohol/entity/AlcoholAlias.java
├── note/entity/Note.java
├── note/entity/NoteImage.java
├── note/entity/Like.java
├── tag/entity/Tag.java
├── tag/entity/NoteTag.java
└── flavor/entity/FlavorSuggestion.java

## 공통
- 모든 엔티티는 BaseEntity 상속 (createdAt, updatedAt)
- @ManyToOne은 fetch = FetchType.LAZY
- Lombok 사용
- @NoArgsConstructor(access = AccessLevel.PROTECTED)
