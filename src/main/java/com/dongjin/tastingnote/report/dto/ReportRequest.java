package com.dongjin.tastingnote.report.dto;

import com.dongjin.tastingnote.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {

    @NotNull(message = "신고 사유는 필수입니다")
    private ReportReason reason;

    private String reasonDetail; // OTHER일 때만 입력
}