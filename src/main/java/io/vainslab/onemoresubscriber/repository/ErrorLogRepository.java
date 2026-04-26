package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}
