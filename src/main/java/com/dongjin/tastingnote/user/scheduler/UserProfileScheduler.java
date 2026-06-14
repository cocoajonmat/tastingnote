package com.dongjin.tastingnote.user.scheduler;

import com.dongjin.tastingnote.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileScheduler {

    private final UserProfileService userProfileService;

    @Scheduled(cron = "0 0 3 * * *")
    public void updateAllProfiles() {
        log.info("사용자 프로파일 갱신 시작");
        userProfileService.buildAllProfiles();
        log.info("사용자 프로파일 갱신 완료");
    }
}