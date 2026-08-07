package com.quantedge.backend.repository;

import com.quantedge.backend.entity.OrderExecution;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderExecutionRepository extends JpaRepository<OrderExecution, UUID> {}
