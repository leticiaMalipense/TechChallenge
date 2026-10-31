package br.com.autoshop.repository.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

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
    @Column(nullable = false, length = 50)
    @ColumnDefault("'Não definido'")
    private String brand = "Não definido";

    @Builder.Default
    @Column(nullable = false, length = 80)
    @ColumnDefault("'Não definido'")
    private String model = "Não definido";

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Integer year = 0;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @Column(nullable = true)
    private LocalDateTime lastReview;

    public VehicleEntity() {
    }
}
