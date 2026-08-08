package com.quantedge.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderExecutionRepository extends JpaRepository<OrderExecution, UUID> {

    List<OrderExecution> findTop10ByOrderUserOrderByExecutedAtDesc(User user);

    List<OrderExecution> findByOrderUserAndExecutedAtLessThanEqualOrderByExecutedAtAsc(User user, Instant asOf);

    @Query("select oe from OrderExecution oe join fetch oe.order o join fetch o.company "
            + "where o.user = :user order by oe.executedAt asc")
    List<OrderExecution> findAllByUserOrderByExecutedAtAsc(@Param("user") User user);
}
