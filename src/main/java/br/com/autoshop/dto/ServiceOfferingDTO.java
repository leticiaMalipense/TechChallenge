package br.com.autoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ServiceOfferingDTO {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Description cannot be empty")
    private String description;

    @Positive
    @NotNull(message = "The base cost cannot be empty")
    private Double baseCost;

    @Positive
    @NotNull(message = "The base cost cannot be empty")
    private Integer estimatedDurationMinutes;

    @NotBlank(message = "Category cannot be empty")
    private String category;

    public ServiceOfferingDTO(){

    }

}
