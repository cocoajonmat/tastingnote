package com.dongjin.tastingnote.report.entity;

public enum ReportReason {
    SPAM,           // 스팸 / 홍보
    INAPPROPRIATE,  // 부적절한 내용
    FALSE_INFO,     // 허위 정보
    OTHER           // 기타 (reasonDetail 필드에 직접 입력)
}