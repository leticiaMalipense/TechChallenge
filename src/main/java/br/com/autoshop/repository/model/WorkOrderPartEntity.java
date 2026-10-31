package br.com.autoshop.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@Entity
@Table(name = "work_order_part", uniqueConstraints = @UniqueConstraint(
        name = "uk_work_order_part", columnNames = {"work_order_id", "part_id"}))
@AllArgsConstructor
public class WorkOrderPartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderEntity workOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private PartEntity part;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    // Snapshot de part.salePrice no momento da inclusão: reajustes posteriores não alteram o orçamento.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    public WorkOrderPartEntity() {
    }
}
