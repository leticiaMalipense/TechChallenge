package br.com.autoshop.dto;

import jakarta.validation.constraints.NotBlank;
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

    private LocalDateTime lastReview;

    public VehicleDTO() {
    }
}
