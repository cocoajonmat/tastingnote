package com.dongjin.tastingnote.event.repository;

import com.dongjin.tastingnote.event.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    List<UserEvent> findAllByUserId(Long userId);
}