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
public class WorkOrderResponseDTO {

    private UUID id;

    private String clientName;

    private String clientDocument;

    private String vehiclePlateNumber;

    private String vehicleBrand;

    private String vehicleModel;

    private Integer vehicleYear;

    private WorkOrderStatus status;

    @Builder.Default
    private List<ServiceOfferingDTO> services = new ArrayList<>();

    // TODO: Incluir lista de peças e insumos quando a entidade for criada

    private Double totalEstimate;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    public WorkOrderResponseDTO() {
    }
}
