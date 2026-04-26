package io.vainslab.onemoresubscriber.repository;

import io.vainslab.onemoresubscriber.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findAllByActiveTrue();

    @Query("SELECT s FROM Service s JOIN FETCH s.createdBy WHERE s.active = true")
    List<Service> findAllActiveWithCreator();
}
