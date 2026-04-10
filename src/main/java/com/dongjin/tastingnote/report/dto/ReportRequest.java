package com.dongjin.tastingnote.report.dto;

import com.dongjin.tastingnote.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {

    @NotNull(message = "신고 사유는 필수입니다")
    private ReportReason reason;

    @Size(max = 500, message = "신고 상세 내용은 500자 이하여야 합니다")
    private String reasonDetail; // OTHER일 때만 입력
}