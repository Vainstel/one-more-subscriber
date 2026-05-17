package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findAllByActiveTrue();

    @Query("SELECT s FROM Service s JOIN FETCH s.createdBy WHERE s.active = true")
    List<Service> findAllActiveWithCreator();

    @Query("SELECT s FROM Service s JOIN FETCH s.createdBy WHERE s.id = :id")
    Optional<Service> findByIdWithCreator(@Param("id") Long id);
}
