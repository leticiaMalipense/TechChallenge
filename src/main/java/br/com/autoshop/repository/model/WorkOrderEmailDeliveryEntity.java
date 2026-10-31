package br.com.autoshop.repository.model;

import br.com.autoshop.util.EmailDeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@Table(name = "work_order_email_delivery")
@AllArgsConstructor
public class WorkOrderEmailDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderEntity workOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailDeliveryStatus status;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    public WorkOrderEmailDeliveryEntity() {
    }
}
