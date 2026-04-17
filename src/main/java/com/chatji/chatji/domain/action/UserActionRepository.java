package com.chatji.chatji.domain.action;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    
    // 특정 유저가 가장 많이 클릭한 카테고리 TOP 3 추출
    @Query("SELECT a.category FROM UserAction a WHERE a.userId = :userId GROUP BY a.category ORDER BY COUNT(a.id) DESC LIMIT 3")
    List<String> findTopCategoriesByUserId(@Param("userId") String userId);
}
