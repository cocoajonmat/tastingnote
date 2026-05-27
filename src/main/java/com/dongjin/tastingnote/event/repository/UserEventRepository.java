package com.dongjin.tastingnote.event.repository;

import com.dongjin.tastingnote.event.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {
}