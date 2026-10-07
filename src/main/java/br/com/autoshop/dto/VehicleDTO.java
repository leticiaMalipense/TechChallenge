package br.com.autoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class VehicleDTO {
  @NotBlank(message = "Plate number cannot be empty.")
  private String plateNumber;
}
