package br.com.autoshop.dto;

import br.com.autoshop.dto.validator.plateNumber.PlateNumber;
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

    @PlateNumber
    @NotBlank(message = "Plate number cannot be empty")
    private String plateNumber;

    private LocalDateTime lastReview;

    public VehicleDTO() {
    }
}
