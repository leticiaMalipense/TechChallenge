package br.com.autoshop.model;

import br.com.autoshop.util.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@Table(name = "workOrder")
@AllArgsConstructor
public class WorkOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status = WorkOrderStatus.RECEIVED;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "work_order_services",
            joinColumns = @JoinColumn(name = "work_order_id"),
            inverseJoinColumns = @JoinColumn(name = "service_offering_id")
    )
    private List<ServiceOfferingEntity> services = new ArrayList<>();

    // TODO: Adicionar peças e insumos quando a respectiva entidade for criada

    @Builder.Default
    @Column(nullable = false)
    private Double totalBudget = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updateDate = LocalDateTime.now();

    public WorkOrderEntity() {
    }
}
