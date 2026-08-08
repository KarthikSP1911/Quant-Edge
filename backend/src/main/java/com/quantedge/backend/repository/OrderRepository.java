package com.quantedge.backend.repository;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCompanyAndStatus(Company company, OrderStatus status);

    List<Order> findByUserAndStatusInOrderByCreatedAtDesc(User user, Collection<OrderStatus> statuses);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    /** Row-level lock held for the duration of the matcher's fill transaction - the double-fill guard. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithLockById(@Param("id") UUID id);
}
