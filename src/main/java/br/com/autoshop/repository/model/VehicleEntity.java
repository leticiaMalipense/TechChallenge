package br.com.autoshop.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "vehicle")
@AllArgsConstructor
public class VehicleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String plateNumber;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(50) default 'Não definido'")
    private String brand = "Não definido";

    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(80) default 'Não definido'")
    private String model = "Não definido";

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer year = 0;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime lastReview;

    public VehicleEntity() {
    }
}
