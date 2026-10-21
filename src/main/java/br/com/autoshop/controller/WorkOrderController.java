package br.com.autoshop.controller;

import br.com.autoshop.dto.WorkOrderRequestDTO;
import br.com.autoshop.dto.WorkOrderResponseDTO;
import br.com.autoshop.dto.WorkOrderTrackingDTO;
import br.com.autoshop.service.WorkOrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SecurityRequirement
@RestController
@RequestMapping("/work-orders")
@Tag(name = "Work Order")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    public ResponseEntity<WorkOrderResponseDTO> create(@Valid @RequestBody WorkOrderRequestDTO dto) {
        WorkOrderResponseDTO created = workOrderService.create(dto);
        return ResponseEntity.created(getLocation(created.getId())).body(created);
    }

    private static @NonNull URI getLocation(UUID id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponseDTO> getById(@PathVariable UUID id) {
        WorkOrderResponseDTO workOrder = workOrderService.getById(id);
        if (Objects.nonNull(workOrder)) {
            return ResponseEntity.ok(workOrder);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/client/{document}")
    public ResponseEntity<List<WorkOrderResponseDTO>> getByClientDocument(@PathVariable String document) {
        List<WorkOrderResponseDTO> workOrders = workOrderService.getByClientDocument(document);
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping
    public ResponseEntity<Page<WorkOrderResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(workOrderService.getAll(pageable));
    }

    @PostMapping("/{id}/services")
    public ResponseEntity<WorkOrderResponseDTO> addServices(
            @PathVariable UUID id,
            @RequestBody List<Long> serviceIds
    ) {
        return ResponseEntity.ok(workOrderService.addServices(id, serviceIds));
    }

    @PatchMapping("/{id}/start-diagnosis")
    public ResponseEntity<WorkOrderResponseDTO> startDiagnosis(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.startDiagnosis(id));
    }

    @PatchMapping("/{id}/send-for-approval")
    public ResponseEntity<WorkOrderResponseDTO> sendForApproval(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.sendForApproval(id));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<WorkOrderResponseDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.approve(id));
    }

    @PatchMapping("/{id}/finish")
    public ResponseEntity<WorkOrderResponseDTO> finish(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.finish(id));
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<WorkOrderResponseDTO> deliver(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.deliver(id));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<WorkOrderTrackingDTO> trackWorkOrder(
            @PathVariable UUID id,
            @RequestParam String documentPrefix
    ) {
        return ResponseEntity.ok(workOrderService.trackWorkOrder(id, documentPrefix));
    }
}
