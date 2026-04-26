package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    Optional<BotUser> findByTelegramId(Long telegramId);
}
