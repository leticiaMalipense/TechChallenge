package br.com.autoshop.service;

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
import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ServiceOfferingRepository serviceOfferingRepository;

    private WorkOrderService workOrderService;

    private ClientEntity defaultClient;
    private VehicleEntity defaultVehicle;
    private ServiceOfferingEntity oilChangeService;
    private ServiceOfferingEntity alignmentService;

    @BeforeEach
    void setUp() {
        workOrderService = new WorkOrderService(
                workOrderRepository,
                clientRepository,
                vehicleRepository,
                serviceOfferingRepository
        );

        defaultClient = ClientEntity.builder()
                .id(1L)
                .name("João Silva")
                .document("123.456.789-00")
                .documentType(DocumentType.CPF)
                .email("joao@email.com")
                .phone("11999999999")
                .status(true)
                .build();

        defaultVehicle = VehicleEntity.builder()
                .id(10L)
                .plateNumber("ABC1D23")
                .brand("Toyota")
                .model("Corolla")
                .year(2022)
                .build();

        oilChangeService = ServiceOfferingEntity.builder()
                .id(100L)
                .name("Troca de Óleo")
                .description("Troca de óleo 5W30 e filtro")
                .baseCost(180.0)
                .estimatedDurationMinutes(45)
                .category("Manutenção")
                .active(true)
                .build();

        alignmentService = ServiceOfferingEntity.builder()
                .id(101L)
                .name("Alinhamento")
                .description("Alinhamento 3D")
                .baseCost(120.0)
                .estimatedDurationMinutes(30)
                .category("Suspensão")
                .active(true)
                .build();
    }

    @Test
    void createWithExistingVehicleAndNoServicesStartsInReceivedStatus() {
        UUID generatedId = UUID.randomUUID();
        WorkOrderRequestDTO request = WorkOrderRequestDTO.builder()
                .clientDocument("123.456.789-00")
                .vehiclePlateNumber("ABC1D23")
                .build();

        when(clientRepository.findByDocument("123.456.789-00")).thenReturn(Optional.of(defaultClient));
        when(vehicleRepository.findByPlateNumber("ABC1D23")).thenReturn(Optional.of(defaultVehicle));
        when(workOrderRepository.save(any(WorkOrderEntity.class))).thenAnswer(inv -> {
            WorkOrderEntity entity = inv.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        WorkOrderResponseDTO response = workOrderService.create(request);

        assertEquals(generatedId, response.getId());
        assertEquals(WorkOrderStatus.RECEIVED, response.getStatus());
        assertEquals(0.0, response.getTotalEstimate());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createWithNewVehicleRegistersVehicleAndCalculatesInitialEstimate() {
        UUID generatedId = UUID.randomUUID();
        WorkOrderRequestDTO request = WorkOrderRequestDTO.builder()
                .clientDocument("123.456.789-00")
                .vehiclePlateNumber("NEW9Z99")
                .vehicleBrand("Honda")
                .vehicleModel("Civic")
                .vehicleYear(2023)
                .serviceIds(List.of(100L, 101L))
                .build();

        when(clientRepository.findByDocument("123.456.789-00")).thenReturn(Optional.of(defaultClient));
        when(vehicleRepository.findByPlateNumber("NEW9Z99")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(VehicleEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceOfferingRepository.getByIdAndActiveIsTrue(100L)).thenReturn(Optional.of(oilChangeService));
        when(serviceOfferingRepository.getByIdAndActiveIsTrue(101L)).thenReturn(Optional.of(alignmentService));
        when(workOrderRepository.save(any(WorkOrderEntity.class))).thenAnswer(inv -> {
            WorkOrderEntity entity = inv.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        WorkOrderResponseDTO response = workOrderService.create(request);

        assertEquals(WorkOrderStatus.IN_DIAGNOSIS, response.getStatus());
        assertEquals(300.0, response.getTotalEstimate());
        assertEquals("NEW9Z99", response.getVehiclePlateNumber());
        assertEquals("Honda", response.getVehicleBrand());
        verify(vehicleRepository).save(any(VehicleEntity.class));
    }

    @Test
    void createThrowsWhenNewVehicleIsMissingBrandModelOrYear() {
        WorkOrderRequestDTO request = WorkOrderRequestDTO.builder()
                .clientDocument("123.456.789-00")
                .vehiclePlateNumber("NEW9Z99")
                .build();

        when(clientRepository.findByDocument("123.456.789-00")).thenReturn(Optional.of(defaultClient));
        when(vehicleRepository.findByPlateNumber("NEW9Z99")).thenReturn(Optional.empty());

        RequestInvalidException ex = assertThrows(RequestInvalidException.class,
                () -> workOrderService.create(request));
        assertEquals(WorkOrderService.VEHICLE_DETAILS_REQUIRED, ex.getMessage());
    }

    @Test
    void fullStatusWorkflowFromReceivedToDeliveredUpdatesStatusAndVehicleLastReview() {
        UUID id = UUID.randomUUID();
        WorkOrderEntity entity = WorkOrderEntity.builder()
                .id(id)
                .client(defaultClient)
                .vehicle(defaultVehicle)
                .status(WorkOrderStatus.RECEIVED)
                .services(new ArrayList<>())
                .totalEstimate(0.0)
                .build();

        when(workOrderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(serviceOfferingRepository.getByIdAndActiveIsTrue(100L)).thenReturn(Optional.of(oilChangeService));
        when(workOrderRepository.save(any(WorkOrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Add service -> transitions RECEIVED to IN_DIAGNOSIS and updates estimate
        WorkOrderResponseDTO afterAddService = workOrderService.addServices(id, List.of(100L));
        assertEquals(WorkOrderStatus.IN_DIAGNOSIS, afterAddService.getStatus());
        assertEquals(180.0, afterAddService.getTotalEstimate());

        // 2. Send for approval -> transitions IN_DIAGNOSIS to AWAITING_APPROVAL
        WorkOrderResponseDTO afterSendApproval = workOrderService.sendForApproval(id);
        assertEquals(WorkOrderStatus.AWAITING_APPROVAL, afterSendApproval.getStatus());

        // 3. Approve -> transitions AWAITING_APPROVAL to IN_EXECUTION
        WorkOrderResponseDTO afterApprove = workOrderService.approve(id);
        assertEquals(WorkOrderStatus.IN_EXECUTION, afterApprove.getStatus());

        // 4. Finish -> transitions IN_EXECUTION to FINISHED
        WorkOrderResponseDTO afterFinish = workOrderService.finish(id);
        assertEquals(WorkOrderStatus.FINISHED, afterFinish.getStatus());

        // 5. Deliver -> transitions FINISHED to DELIVERED and updates vehicle lastReview
        WorkOrderResponseDTO afterDeliver = workOrderService.deliver(id);
        assertEquals(WorkOrderStatus.DELIVERED, afterDeliver.getStatus());
        assertNotNull(defaultVehicle.getLastReview());
        verify(vehicleRepository).save(defaultVehicle);
    }

    @Test
    void trackWorkOrderReturnsTrackingDtoWhenApprovedAndDocumentPrefixMatches() {
        UUID id = UUID.randomUUID();
        WorkOrderEntity entity = WorkOrderEntity.builder()
                .id(id)
                .client(defaultClient)
                .vehicle(defaultVehicle)
                .status(WorkOrderStatus.IN_EXECUTION)
                .services(List.of(oilChangeService))
                .totalEstimate(180.0)
                .build();

        when(workOrderRepository.findById(id)).thenReturn(Optional.of(entity));

        WorkOrderTrackingDTO tracking = workOrderService.trackWorkOrder(id, "123");

        assertEquals(id, tracking.getId());
        assertEquals(WorkOrderStatus.IN_EXECUTION, tracking.getStatus());
        assertEquals(List.of("Troca de Óleo"), tracking.getServiceNames());
    }

    @Test
    void trackWorkOrderThrowsWhenDocumentPrefixDoesNotMatch() {
        UUID id = UUID.randomUUID();
        WorkOrderEntity entity = WorkOrderEntity.builder()
                .id(id)
                .client(defaultClient)
                .vehicle(defaultVehicle)
                .status(WorkOrderStatus.IN_EXECUTION)
                .services(List.of(oilChangeService))
                .totalEstimate(180.0)
                .build();

        when(workOrderRepository.findById(id)).thenReturn(Optional.of(entity));

        RequestInvalidException ex = assertThrows(RequestInvalidException.class,
                () -> workOrderService.trackWorkOrder(id, "999"));
        assertEquals(WorkOrderService.INVALID_DOCUMENT_PREFIX, ex.getMessage());
    }

    @Test
    void trackWorkOrderThrowsWhenWorkOrderIsNotYetApproved() {
        UUID id = UUID.randomUUID();
        WorkOrderEntity entity = WorkOrderEntity.builder()
                .id(id)
                .client(defaultClient)
                .vehicle(defaultVehicle)
                .status(WorkOrderStatus.AWAITING_APPROVAL)
                .services(List.of(oilChangeService))
                .totalEstimate(180.0)
                .build();

        when(workOrderRepository.findById(id)).thenReturn(Optional.of(entity));

        RequestInvalidException ex = assertThrows(RequestInvalidException.class,
                () -> workOrderService.trackWorkOrder(id, "123"));
        assertEquals(WorkOrderService.WORK_ORDER_NOT_APPROVED_YET, ex.getMessage());
    }
}
