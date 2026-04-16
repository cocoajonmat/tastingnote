새 세션을 시작합니다. 아래 순서대로 진행하세요.

## 1. 프로젝트 문서 파악

다음 파일들을 **Read 툴로 직접** 읽으세요. 서브에이전트(Agent 툴) 사용 금지.

- `context.md` — 현재 진행 상황, 브랜치, 다음 할 일
- `CHANGELOG.md` — 최근 변경 이력 (마지막 2~3개 섹션만)
- `FEATURES.md` — 전체 기능 구현 현황
- `LEARNING.md` — 지금까지 배운 개념 목록
- `COLLABORATION.md` — 협업 방식 및 규칙

memory 파일들도 읽으세요:
- `C:\Users\gkehd\.claude\projects\C--Users-gkehd-projects-tastingnote\memory\project_overview.md`
- `C:\Users\gkehd\.claude\projects\C--Users-gkehd-projects-tastingnote\memory\project_decisions.md`
- `C:\Users\gkehd\.claude\projects\C--Users-gkehd-projects-tastingnote\memory\project_session.md`

## 2. Git 상태 확인

다음 명령어들을 실행해서 커밋/푸시 누락 여부를 확인하세요:

```bash
git status
git branch -vv
git log --oneline -10
git stash list
```

확인 후 아래 항목을 파악하세요:
- 현재 브랜치가 무엇인지
- 스테이징 안 된 변경 파일이 있는지
- 커밋은 됐지만 push 안 된 커밋이 있는지
- stash에 임시 저장된 작업이 있는지

## 3. 소스코드 현황 파악

`context.md`에서 현재 브랜치/작업 대상을 확인한 뒤, 관련 파일들을 **Glob/Grep으로** 빠르게 훑으세요.
(전체 파일을 다 읽지 말고, 필요한 파일만 타겟해서 확인)

```bash
# 예: 최근 변경된 소스 파일 확인
git diff --name-only HEAD~3 HEAD
```

## 4. 세션 시작 브리핑 출력

위 내용을 바탕으로 아래 형식으로 브리핑을 출력하세요.
**반드시 코드블록(`\`\`\``) 없이 마크다운 원문 그대로 출력하세요. `#`, `##` 기호가 그대로 보여야 합니다.**

```
# N회차 세션 시작 — YYYY-MM-DD

## 📍 현재 위치
- 브랜치: `브랜치명`
- 마지막 작업: 한 줄 요약

## ✅ 지난 세션까지 완료된 것
- 완료 항목 나열

## 🔧 현재 진행 중 / 미완성
- 미완성 항목 나열

## ⚠️ Git 주의사항
- push 안 된 커밋: 있음/없음 (있으면 목록)
- 미커밋 변경: 있음/없음 (있으면 목록)
- stash: 있음/없음

## ⏭️ 오늘 할 일 제안
context.md와 FEATURES.md를 보고 자연스러운 다음 작업 2~3개 제안

## 💡 참고 사항
(놓치기 쉬운 것, 이전 세션에서 보류된 결정, 주의할 의존성 등 있으면 기재)
```

브리핑 출력 후, 유저에게 오늘 무엇을 할지 물어보세요.