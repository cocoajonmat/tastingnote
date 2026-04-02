# 협업 가이드

이 파일은 Claude Code와 함께 TastingNote 프로젝트를 진행할 때의 협업 방식을 정리한 문서입니다.
새 세션 시작 시 반드시 이 파일과 `context.md`를 먼저 읽고 시작하세요.

---

## 유저 수준 및 배경

- **Spring Boot**: 첫 번째 프로젝트 (Java 문법은 알고 있음)
- **Git / GitHub**: 현재 처음 사용 중 — 브랜치, PR, CI/CD 개념 모두 학습 단계
- **목표**: 단순히 완성하는 것이 아니라, 실무에서 쓰이는 방식을 배우면서 개발하는 것

→ 코드 작성 전 항상 "왜 이 방식인지", "대안은 무엇인지"를 설명해줘야 한다.
→ Git 관련 작업 시 명령어만 주지 말고, 이 명령어가 어떤 의미인지 함께 설명한다.

---

## 협업 방식

### 기본 원칙
- **토론 먼저, 코드는 나중**: 구현 방향을 합의한 뒤에 코드를 작성한다.
- **선택지 제시**: "A 방식 vs B 방식 — 이 프로젝트에선 이런 이유로 A를 추천합니다. 어떻게 생각하세요?" 형식으로 질문한다.
- **이유 포함 필수**: 기능/방식 설명 시 성능, DB 부하, 유지보수성 등 근거를 항상 포함한다.
- **세세하게 질문**: 모르는 개념이 있을 수 있으니 중간중간 확인하며 진행한다.

### 세션 흐름
1. 세션 시작 → `context.md` + `COLLABORATION.md` 읽기
2. 현황 파악 후 "오늘 뭘 할지" 함께 결정
3. 구현 전 토론 → 방향 합의
4. 코드 작성
5. 유저가 **"오늘은 여기까지"** 라고 하면 → `context.md` 업데이트 도와주기

---

## Git / GitHub 활용 방향

### 현재 상태 (2026-04-02 기준)
- `main` + `feature/*` 브랜치 전략 도입 완료
- 현재 작업 브랜치: `feature/jwt-auth`
- `main`에 push하면 GitHub Actions가 자동으로 서버에 배포됨
- 개발 환경: 노트북 → 데스크탑으로 전환 완료 (GitHub에서 브랜치 fetch)

### 브랜치 전략 (확정, 2026-04-01)
- 기능 개발마다 별도 브랜치를 만들어서 작업
- 완성되면 `main`에 PR(Pull Request)을 열고 머지
- 예시:
  ```
  feature/jwt-auth       ← JWT 인증 개발 (완료)
  feature/alcohol-api    ← 술 검색 API 개발
  feature/tag-api        ← 태그 기능 개발
  ```
- **왜 이렇게 하나?**
  - 기능이 완성되기 전까지 서버에 배포되지 않음 (안전)
  - 어떤 기능을 언제 만들었는지 히스토리가 명확해짐
  - 나중에 팀 협업을 하게 될 때도 그대로 쓸 수 있는 실무 방식

---

## CI/CD 구조 (현재)

- **도구**: GitHub Actions
- **파일 위치**: `.github/workflows/deploy.yml`
- **동작 방식**:
  1. `main` 브랜치에 push
  2. GitHub Actions가 자동으로 JAR 빌드
  3. SSH로 AWS Lightsail 서버에 JAR 전송
  4. `systemctl restart tastingnote` 로 서버 재시작
- **서버**: AWS Lightsail (`13.124.79.235`)
- **Swagger UI**: `http://13.124.79.235:8080/tastingnote.swagger`

---

## 개발 방향

- **방식**: 탑다운 (엔티티 → Repository → Service → Controller)
- **현재까지**: 엔티티 전체 + Note CRUD + JWT 인증 + ApiResponse + Report 완료
- **다음 순서**: Alcohol → Tag → Like → FlavorSuggestion
- **기능 목록**: `FEATURES.md` 참고 (친구와 구상한 초안, 세션마다 업데이트)

---

## context.md 업데이트 규칙

세션 마무리 시 아래 항목 중 변경된 것을 `context.md`에 반영한다:
- 새로 확정된 설계 결정
- 새로 구현 완료된 기능
- 보류에서 확정으로 바뀐 항목
- 새로 생긴 보류 항목