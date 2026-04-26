package io.vainslab.onemoresubscriber.service;

import io.vainslab.onemoresubscriber.entity.BotUser;
import io.vainslab.onemoresubscriber.repository.BotUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class BotUserService {

    private final BotUserRepository botUserRepository;

    @Transactional
    public BotUser save(BotUser user) {
        return botUserRepository.save(user);
    }

    @Transactional
    public BotUser getOrCreate(User telegramUser) {
        return botUserRepository.findByTelegramId(telegramUser.getId())
                .map(existing -> {
                    existing.setUsername(telegramUser.getUserName());
                    existing.setFirstName(telegramUser.getFirstName());
                    existing.setLastName(telegramUser.getLastName());
                    return botUserRepository.save(existing);
                })
                .orElseGet(() -> {
                    BotUser user = new BotUser(
                            telegramUser.getId(),
                            telegramUser.getFirstName(),
                            telegramUser.getLastName(),
                            telegramUser.getUserName()
                    );
                    return botUserRepository.save(user);
                });
    }
}
