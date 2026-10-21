package br.com.autoshop.dto;

import br.com.autoshop.util.WorkOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class WorkOrderTrackingDTO {

    private UUID id;

    private String vehiclePlateNumber;

    private String vehicleBrand;

    private String vehicleModel;

    private WorkOrderStatus status;

    @Builder.Default
    private List<String> serviceNames = new ArrayList<>();

    private Double totalBudget;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    public WorkOrderTrackingDTO() {
    }
}
