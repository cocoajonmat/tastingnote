지금까지 확정된 설계 내용을 정리해줄게.
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

### Alcohol
- name(영문), nameKo(한글) 컬럼 분리
- AlcoholAlias 테이블 별도 (별칭 검색용)
    - 예: "블랙라벨", "JW Black" → "조니워커 블랙라벨"로 매칭
- DB에 없는 술 → Note의 alcoholName 필드에 자유 텍스트 저장 (alcohol 필드 null)
- 술 등록 요청 기능은 추후 추가

### Note
- alcohol 필드 (@ManyToOne, nullable) → DB에 있는 술
- alcoholName 필드 (String, nullable) → DB에 없는 술 직접 입력
- taste, aroma → C방식 (제안 목록 + 자유 입력 둘 다 허용)
- pairing, description → 자유 텍스트
- is_public → 공개/비공개 토글

### Like (반응)
- LikeType으로 반응 종류 구분 (좋아요, 최고에요 등)
- 노트당 하나만 선택 가능

### FlavorSuggestion (새 테이블 추가)
- taste/aroma 입력 시 제안 목록용
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

### 피드
- 공개 노트 피드
- 정렬: 좋아요 많은 순(기본) + 최신순 선택 가능

### 보류 항목 (지금 구현 안 함)
- 팔로우 / 팔로워
- 댓글
- 알림
- unpublish (공개/비공개 토글로 대체)

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