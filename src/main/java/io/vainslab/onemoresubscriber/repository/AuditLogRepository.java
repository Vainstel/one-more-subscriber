package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
