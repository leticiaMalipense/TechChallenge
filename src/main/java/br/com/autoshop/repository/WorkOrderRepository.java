package br.com.autoshop.repository;

import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.util.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, UUID> {

    List<WorkOrderEntity> findByClient_Document(String document);

    List<WorkOrderEntity> findByVehicle_PlateNumber(String plateNumber);

    List<WorkOrderEntity> findByStatus(WorkOrderStatus status);

    @Modifying
    @Query("""
            update WorkOrderEntity workOrder
            set workOrder.status = :targetStatus, workOrder.updateDate = :updatedAt
            where workOrder.id = :id and workOrder.status = :expectedStatus
            """)
    int transitionStatus(
            @Param("id") UUID id,
            @Param("expectedStatus") WorkOrderStatus expectedStatus,
            @Param("targetStatus") WorkOrderStatus targetStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
