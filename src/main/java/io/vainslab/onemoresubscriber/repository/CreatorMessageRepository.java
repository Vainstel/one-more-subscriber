package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.CreatorMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorMessageRepository extends JpaRepository<CreatorMessage, Long> {
}
