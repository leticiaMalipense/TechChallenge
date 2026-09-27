package br.com.autoshop.model;

import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "part")
@AllArgsConstructor
public class PartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, unique = true)
    private String code;

    private String category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PartType partType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PartUnit unit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    public PartEntity() {
    }
}
