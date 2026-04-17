package com.chatji.chatji.domain.hotdeal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HotDealRepository extends JpaRepository<HotDeal, Long> {
    Optional<HotDeal> findByOriginId(String originId);
}
