package br.com.autoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class VehicleDTO {

    @NotBlank(message = "Plate number cannot be empty")
    private String plateNumber;

    @NotBlank(message = "Brand cannot be empty")
    private String brand;

    @NotBlank(message = "Model cannot be empty")
    private String model;

    @NotNull(message = "Year cannot be null")
    private Integer year;

    private LocalDateTime lastReview;

    public VehicleDTO() {
    }
}
