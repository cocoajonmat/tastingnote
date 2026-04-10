# 변경/개선 이유 기록

코드를 왜 이렇게 바꿨는지를 기록하는 파일.
context.md 완료 섹션은 "무엇을 했는지"만 기록하고,
이유가 필요한 결정은 여기에 남긴다.

---

## 2026-04-09 — Note 엔티티 alcoholName 자유입력 필드 제거

**결정**: alcoholName String 필드 제거, alcohol @ManyToOne nullable=false로 변경 (엄격한 방식)

**이유**: 자유입력 허용 시 같은 술이 "조니워커", "JW Black", "조니워커 블랙" 등 제각각 저장됨.
이렇게 되면 "이 술을 마신 사람이 몇 명인지", "이 술의 평균 별점" 같은 기능이 불가능해짐.
Discovery/술 상세 페이지/통계 등 핵심 기능이 처음부터 막히는 구조.

**대안**: DB에 없는 술은 AlcoholRequest로 등록 요청 → 관리자 승인 후 노트 작성 가능.

---

## 2026-04-10 — AlcoholCategory에 한글명 추가

**결정**: AlcoholCategory enum에 nameKo 필드와 findByNameKo() 메서드 추가

**이유**: 검색창에 "위스키"를 입력해도 결과가 없었음.
기존 검색은 name(영문) + nameKo(한글) + alias만 체크하고 카테고리명은 포함하지 않았음.
AlcoholCategory enum이 WHISKEY처럼 영문이라 "위스키"와 매칭되지 않아서 해당 카테고리 전체가 누락됨.

---

## 2026-04-10 — FlavorSuggestion 응답에 id 포함

**결정**: FlavorSuggestionResponse를 name(String)만 반환하던 것에서 id + name으로 변경

**이유**: 노트 작성 시 클라이언트가 맛/향을 선택하면 해당 FlavorSuggestion의 id(tasteIds, aromaIds)를 보내야 함.
id 없이 이름만 반환하면 클라이언트가 노트 저장 요청을 만들 수 없음.

---

## 2026-04-10 — 비로그인 유저 노트 상세 조회 허용

**결정**: SecurityConfig에 RegexRequestMatcher("/api/notes/\\d+", "GET").permitAll() 추가.
NoteController.getNote()에서 userId를 선택적으로 추출 (비로그인 시 null).

**이유**: 서비스의 기본 방향이 공개 피드/노트 조회는 비로그인도 가능해야 함.
기존에는 /api/notes/{noteId} GET도 인증 필요해서 비로그인 유저가 아무것도 볼 수 없었음.
단, /api/notes/my와 충돌하지 않도록 숫자 ID만 허용하는 정규식 사용.

---

## 2026-04-10 — 비공개/DRAFT 노트 신고 차단

**결정**: ReportService에서 note.getStatus() == DRAFT || !note.getIsPublic() 이면 403 반환

**이유**: 비공개 노트나 임시저장 노트는 본인 외에 볼 수 없는 콘텐츠.
신고 기능은 다른 유저가 볼 수 있는 공개 콘텐츠에 대해서만 의미 있음.
비공개 노트를 신고할 수 있으면 노트 존재 여부가 간접적으로 노출되는 보안 문제도 있음.

---

## 2026-04-10 — reason=OTHER 시 reasonDetail 필수화

**결정**: ReportService에서 reason == OTHER && reasonDetail이 null/blank이면 INVALID_INPUT 에러

**이유**: OTHER(기타)는 정해진 사유 외의 신고를 위한 항목.
reasonDetail 없이 OTHER를 선택하면 관리자가 어떤 문제인지 파악 불가 → 처리가 불가능한 신고가 DB에 쌓임.

---

## 2026-04-10 — 탈퇴 유저 토큰 재발급 차단

**결정**: UserService.reissue()에서 refreshToken.getUser().getDeletedAt() != null이면 USER_NOT_FOUND 에러

**이유**: Refresh Token은 30일간 유효한데, 탈퇴(deletedAt 기록) 후에도 기존 토큰으로 재발급이 가능했음.
탈퇴한 유저가 Access Token을 계속 받을 수 있으면 탈퇴의 의미가 없음.
로그인(login)은 이미 findByEmailAndDeletedAtIsNull로 차단되어 있지만, reissue는 이메일 조회를 거치지 않아 누락됨.

---

## 2026-04-10 — 닉네임 공백 검증 추가

**결정**: SignUpRequest nickname 필드에 @Pattern(regexp = "^\\S+$") 추가

**이유**: @NotBlank는 전체가 공백인 경우만 막음 (" " → 차단, "닉 네임" → 통과).
닉네임 중간에 공백이 있으면 표시될 때 어색하고, 검색/자동완성 기능에서 예상치 못한 동작 가능.

---

## 2026-04-10 — 비밀번호 복잡도 검증 추가

**결정**: SignUpRequest password 필드에 @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") 추가

**이유**: 8자 이상 조건(@Size)만으로는 "aaaaaaaa", "12345678" 같은 단순 비밀번호가 허용됨.
최소한 영문자+숫자 조합을 강제해야 브루트포스 공격에 덜 취약함.
특수문자는 강제하지 않음 — 입력 UX와 보안의 균형을 고려한 결정.

---

## 2026-04-10 — Swagger createNote 설명 오류 수정

**결정**: NoteController createNote @Operation description에서 "status 필드로 DRAFT/PUBLISHED 선택 가능" 문구 제거

**이유**: 실제 동작은 생성 시 항상 DRAFT로 저장되며, status 필드는 NoteCreateRequest에 존재하지 않음.
설명과 실제 동작이 다르면 프론트(친구)가 없는 기능을 구현하려고 시간을 낭비할 수 있음.