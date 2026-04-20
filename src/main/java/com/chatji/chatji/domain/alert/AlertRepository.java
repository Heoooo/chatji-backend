package com.chatji.chatji.domain.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByActiveTrue();
    List<Alert> findByUserId(String userId);
}
