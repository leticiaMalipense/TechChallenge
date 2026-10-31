package br.com.autoshop.repository;

import br.com.autoshop.repository.model.WorkOrderEmailDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderEmailDeliveryRepository extends JpaRepository<WorkOrderEmailDeliveryEntity, UUID> {

    long countByWorkOrder_Id(UUID workOrderId);

    Optional<WorkOrderEmailDeliveryEntity> findTopByWorkOrder_IdOrderByCreatedAtDesc(UUID workOrderId);
}
