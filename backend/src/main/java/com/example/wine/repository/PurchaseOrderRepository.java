package com.example.wine.repository;

import com.example.wine.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder join fetch purchaseOrder.wine "
            + "order by purchaseOrder.deliveryDate asc, purchaseOrder.id asc")
    List<PurchaseOrder> findAllWithWineOrderByDeliveryDate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder where purchaseOrder.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);
}