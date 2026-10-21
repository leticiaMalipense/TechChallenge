package br.com.autoshop.repository;

import br.com.autoshop.model.WorkOrderEntity;
import br.com.autoshop.util.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, UUID> {

    List<WorkOrderEntity> findByClient_Document(String document);

    List<WorkOrderEntity> findByVehicle_PlateNumber(String plateNumber);

    List<WorkOrderEntity> findByStatus(WorkOrderStatus status);
}
