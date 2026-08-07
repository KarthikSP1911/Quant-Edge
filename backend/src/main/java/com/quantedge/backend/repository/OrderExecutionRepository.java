package com.quantedge.backend.repository;

import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderExecutionRepository extends JpaRepository<OrderExecution, UUID> {

    List<OrderExecution> findTop10ByOrderUserOrderByExecutedAtDesc(User user);
}
