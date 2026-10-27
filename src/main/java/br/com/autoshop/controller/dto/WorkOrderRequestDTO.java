package br.com.autoshop.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class WorkOrderRequestDTO {

    @NotBlank(message = "Client document cannot be empty")
    private String clientDocument;

    @NotBlank(message = "Vehicle plate number cannot be empty")
    private String vehiclePlateNumber;

    private String vehicleBrand;

    private String vehicleModel;

    private Integer vehicleYear;

    @Builder.Default
    private List<Long> serviceIds = new ArrayList<>();

    public WorkOrderRequestDTO() {
    }
}
