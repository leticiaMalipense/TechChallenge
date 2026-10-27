package br.com.autoshop.service;

import br.com.autoshop.controller.dto.ServiceOfferingDTO;
import br.com.autoshop.controller.dto.WorkOrderRequestDTO;
import br.com.autoshop.controller.dto.WorkOrderResponseDTO;
import br.com.autoshop.controller.dto.WorkOrderTrackingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.ClientRepository;
import br.com.autoshop.repository.ServiceOfferingRepository;
import br.com.autoshop.repository.VehicleRepository;
import br.com.autoshop.repository.WorkOrderRepository;
import br.com.autoshop.repository.model.ClientEntity;
import br.com.autoshop.repository.model.ServiceOfferingEntity;
import br.com.autoshop.repository.model.VehicleEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.util.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkOrderService {

    public static final String CLIENT_NOT_FOUND = "Client not found for the provided document";
    public static final String VEHICLE_DETAILS_REQUIRED = "Vehicle brand, model and year are required when registering a new vehicle";
    public static final String SERVICE_OFFERING_NOT_FOUND = "Service offering not found or inactive: ";
    public static final String WORK_ORDER_NOT_FOUND = "Work order not found";
    public static final String INVALID_STATUS_TRANSITION = "Invalid status transition from ";
    public static final String SERVICES_REQUIRED_FOR_APPROVAL = "At least one service must be added before sending for approval";
    public static final String INVALID_DOCUMENT_PREFIX = "Invalid client document prefix (must be the first 3 digits)";
    public static final String WORK_ORDER_NOT_APPROVED_YET = "Work order tracking is only available after client approval";

    private static final Set<WorkOrderStatus> POST_APPROVAL_STATUSES = EnumSet.of(
            WorkOrderStatus.IN_EXECUTION,
            WorkOrderStatus.FINISHED,
            WorkOrderStatus.DELIVERED
    );

    private final WorkOrderRepository workOrderRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            ClientRepository clientRepository,
            VehicleRepository vehicleRepository,
            ServiceOfferingRepository serviceOfferingRepository
    ) {
        this.workOrderRepository = workOrderRepository;
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    public WorkOrderResponseDTO create(WorkOrderRequestDTO dto) {
        ClientEntity client = clientRepository.findByDocument(dto.getClientDocument())
                .filter(c -> Boolean.TRUE.equals(c.getStatus()))
                .orElseThrow(() -> new RequestInvalidException(CLIENT_NOT_FOUND));

        VehicleEntity vehicle = resolveOrCreateVehicle(dto);
        List<ServiceOfferingEntity> services = resolveServices(dto.getServiceIds());

        WorkOrderStatus initialStatus = services.isEmpty()
                ? WorkOrderStatus.RECEIVED
                : WorkOrderStatus.IN_DIAGNOSIS;

        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .client(client)
                .vehicle(vehicle)
                .services(services)
                .status(initialStatus)
                .build();

        workOrder.setTotalEstimate(calculateEstimate(workOrder));

        WorkOrderEntity saved = workOrderRepository.save(workOrder);
        return convertToResponseDto(saved);
    }

    public WorkOrderResponseDTO getById(UUID id) {
        return workOrderRepository.findById(id)
                .map(this::convertToResponseDto)
                .orElse(null);
    }

    public List<WorkOrderResponseDTO> getByClientDocument(String document) {
        return workOrderRepository.findByClient_Document(document).stream()
                .map(this::convertToResponseDto)
                .toList();
    }

    public Page<WorkOrderResponseDTO> getAll(Pageable pageable) {
        return workOrderRepository.findAll(pageable)
                .map(this::convertToResponseDto);
    }

    public WorkOrderResponseDTO addServices(UUID id, List<Long> serviceIds) {
        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);

        if (workOrder.getStatus() != WorkOrderStatus.RECEIVED
                && workOrder.getStatus() != WorkOrderStatus.IN_DIAGNOSIS) {
            throw new RequestInvalidException(
                    INVALID_STATUS_TRANSITION + workOrder.getStatus() + " when adding services"
            );
        }

        List<ServiceOfferingEntity> newServices = resolveServices(serviceIds);
        if (workOrder.getServices() == null) {
            workOrder.setServices(new ArrayList<>());
        }
        workOrder.getServices().addAll(newServices);

        // TODO: Permitir também a adição de peças e insumos necessários quando a entidade for implementada

        workOrder.setStatus(WorkOrderStatus.IN_DIAGNOSIS);
        workOrder.setTotalEstimate(calculateEstimate(workOrder));
        workOrder.setUpdateDate(LocalDateTime.now());

        return convertToResponseDto(workOrderRepository.save(workOrder));
    }

    public WorkOrderResponseDTO startDiagnosis(UUID id) {
        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);
        validateTransition(workOrder, WorkOrderStatus.RECEIVED, WorkOrderStatus.IN_DIAGNOSIS);

        workOrder.setStatus(WorkOrderStatus.IN_DIAGNOSIS);
        workOrder.setUpdateDate(LocalDateTime.now());
        return convertToResponseDto(workOrderRepository.save(workOrder));
    }

    public WorkOrderResponseDTO sendForApproval(UUID id) {
        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);
        validateTransition(workOrder, WorkOrderStatus.IN_DIAGNOSIS, WorkOrderStatus.AWAITING_APPROVAL);

        if (workOrder.getServices() == null || workOrder.getServices().isEmpty()) {
            throw new RequestInvalidException(SERVICES_REQUIRED_FOR_APPROVAL);
        }

        workOrder.setTotalEstimate(calculateEstimate(workOrder));
        workOrder.setStatus(WorkOrderStatus.AWAITING_APPROVAL);
        workOrder.setUpdateDate(LocalDateTime.now());

        WorkOrderEntity saved = workOrderRepository.save(workOrder);
        sendEstimateToClient(saved);

        return convertToResponseDto(saved);
    }

    public WorkOrderResponseDTO approve(UUID id) {
        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);
        validateTransition(workOrder, WorkOrderStatus.AWAITING_APPROVAL, WorkOrderStatus.IN_EXECUTION);

        workOrder.setStatus(WorkOrderStatus.IN_EXECUTION);
        workOrder.setUpdateDate(LocalDateTime.now());
        return convertToResponseDto(workOrderRepository.save(workOrder));
    }

    public WorkOrderResponseDTO finish(UUID id) {
        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);
        validateTransition(workOrder, WorkOrderStatus.IN_EXECUTION, WorkOrderStatus.FINISHED);

        workOrder.setStatus(WorkOrderStatus.FINISHED);
        workOrder.setUpdateDate(LocalDateTime.now());
        return convertToResponseDto(workOrderRepository.save(workOrder));
    }

    public WorkOrderResponseDTO deliver(UUID id) {
        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);
        validateTransition(workOrder, WorkOrderStatus.FINISHED, WorkOrderStatus.DELIVERED);

        LocalDateTime now = LocalDateTime.now();
        workOrder.setStatus(WorkOrderStatus.DELIVERED);
        workOrder.setUpdateDate(now);

        VehicleEntity vehicle = workOrder.getVehicle();
        if (Objects.nonNull(vehicle)) {
            vehicle.setLastReview(now);
            vehicleRepository.save(vehicle);
        }

        return convertToResponseDto(workOrderRepository.save(workOrder));
    }

    public WorkOrderTrackingDTO trackWorkOrder(UUID id, String documentPrefix) {
        if (Objects.isNull(documentPrefix)) {
            throw new RequestInvalidException(INVALID_DOCUMENT_PREFIX);
        }

        String sanitizedPrefix = documentPrefix.replaceAll("\\D", "");
        if (sanitizedPrefix.length() != 3) {
            throw new RequestInvalidException(INVALID_DOCUMENT_PREFIX);
        }

        WorkOrderEntity workOrder = getWorkOrderOrThrow(id);
        String clientDocDigits = workOrder.getClient().getDocument().replaceAll("\\D", "");

        if (clientDocDigits.length() < 3 || !clientDocDigits.startsWith(sanitizedPrefix)) {
            throw new RequestInvalidException(INVALID_DOCUMENT_PREFIX);
        }

        if (!POST_APPROVAL_STATUSES.contains(workOrder.getStatus())) {
            throw new RequestInvalidException(WORK_ORDER_NOT_APPROVED_YET);
        }

        return convertToTrackingDto(workOrder);
    }

    public Double calculateEstimate(WorkOrderEntity workOrder) {
        double servicesTotal = 0.0;
        if (Objects.nonNull(workOrder.getServices())) {
            servicesTotal = workOrder.getServices().stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(s -> Objects.nonNull(s.getBaseCost()) ? s.getBaseCost() : 0.0)
                    .sum();
        }

        // TODO: Somar o valor das peças e insumos vinculados à WorkOrder quando a entidade for criada
        double partsAndSuppliesTotal = 0.0;

        return servicesTotal + partsAndSuppliesTotal;
    }

    protected void sendEstimateToClient(WorkOrderEntity workOrder) {
        // TODO: Implementar envio automático do orçamento (e-mail/notificação) ao cliente para aprovação
    }

    private VehicleEntity resolveOrCreateVehicle(WorkOrderRequestDTO dto) {
        Optional<VehicleEntity> existingVehicle = vehicleRepository.findByPlateNumber(dto.getVehiclePlateNumber());
        if (existingVehicle.isPresent()) {
            return existingVehicle.get();
        }

        if (Objects.isNull(dto.getVehicleBrand()) || dto.getVehicleBrand().isBlank()
                || Objects.isNull(dto.getVehicleModel()) || dto.getVehicleModel().isBlank()
                || Objects.isNull(dto.getVehicleYear())) {
            throw new RequestInvalidException(VEHICLE_DETAILS_REQUIRED);
        }

        VehicleEntity newVehicle = VehicleEntity.builder()
                .plateNumber(dto.getVehiclePlateNumber())
                .brand(dto.getVehicleBrand())
                .model(dto.getVehicleModel())
                .year(dto.getVehicleYear())
                .build();

        return vehicleRepository.save(newVehicle);
    }

    private List<ServiceOfferingEntity> resolveServices(List<Long> serviceIds) {
        List<ServiceOfferingEntity> services = new ArrayList<>();
        if (Objects.isNull(serviceIds) || serviceIds.isEmpty()) {
            return services;
        }

        for (Long serviceId : serviceIds) {
            ServiceOfferingEntity offering = serviceOfferingRepository.getByIdAndActiveIsTrue(serviceId)
                    .orElseThrow(() -> new RequestInvalidException(SERVICE_OFFERING_NOT_FOUND + serviceId));
            services.add(offering);
        }
        return services;
    }

    private WorkOrderEntity getWorkOrderOrThrow(UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new RequestInvalidException(WORK_ORDER_NOT_FOUND));
    }

    private void validateTransition(WorkOrderEntity workOrder, WorkOrderStatus expectedCurrent, WorkOrderStatus target) {
        if (workOrder.getStatus() != expectedCurrent) {
            throw new RequestInvalidException(
                    INVALID_STATUS_TRANSITION + workOrder.getStatus() + " to " + target
            );
        }
    }

    private WorkOrderResponseDTO convertToResponseDto(WorkOrderEntity entity) {
        List<ServiceOfferingDTO> serviceDtos = entity.getServices() == null
                ? List.of()
                : entity.getServices().stream()
                .map(s -> ServiceOfferingDTO.builder()
                        .name(s.getName())
                        .description(s.getDescription())
                        .baseCost(s.getBaseCost())
                        .estimatedDurationMinutes(s.getEstimatedDurationMinutes())
                        .category(s.getCategory())
                        .build())
                .toList();

        return WorkOrderResponseDTO.builder()
                .id(entity.getId())
                .clientName(entity.getClient().getName())
                .clientDocument(entity.getClient().getDocument())
                .vehiclePlateNumber(entity.getVehicle().getPlateNumber())
                .vehicleBrand(entity.getVehicle().getBrand())
                .vehicleModel(entity.getVehicle().getModel())
                .vehicleYear(entity.getVehicle().getYear())
                .status(entity.getStatus())
                .services(serviceDtos)
                .totalEstimate(entity.getTotalEstimate())
                .creationDate(entity.getCreationDate())
                .updateDate(entity.getUpdateDate())
                .build();
    }

    private WorkOrderTrackingDTO convertToTrackingDto(WorkOrderEntity entity) {
        List<String> serviceNames = entity.getServices() == null
                ? List.of()
                : entity.getServices().stream()
                .map(ServiceOfferingEntity::getName)
                .toList();

        return WorkOrderTrackingDTO.builder()
                .id(entity.getId())
                .vehiclePlateNumber(entity.getVehicle().getPlateNumber())
                .vehicleBrand(entity.getVehicle().getBrand())
                .vehicleModel(entity.getVehicle().getModel())
                .status(entity.getStatus())
                .serviceNames(serviceNames)
                .totalEstimate(entity.getTotalEstimate())
                .creationDate(entity.getCreationDate())
                .updateDate(entity.getUpdateDate())
                .build();
    }
}
