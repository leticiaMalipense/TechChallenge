package br.com.autoshop.controller;

import br.com.autoshop.dto.ServiceOfferingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.ServiceOfferingService;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/service-offering")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @PostMapping
    public ResponseEntity<Void> postServiceOffering(@Valid @RequestBody ServiceOfferingDTO dto) {
        ServiceOfferingDTO serviceOffering = serviceOfferingService.getByName(dto.getName());
        if (Objects.nonNull(serviceOffering)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Long id = serviceOfferingService.save(dto);
        return ResponseEntity.created(getLocation(id)).build();
    }

    private static @NonNull URI getLocation(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOfferingDTO> getServiceOffering(@PathVariable Long id) {
        ServiceOfferingDTO serviceOffering = serviceOfferingService.getById(id);
        if (Objects.nonNull(serviceOffering)) {
            return ResponseEntity.ok(serviceOffering);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Page<ServiceOfferingDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "name") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<ServiceOfferingDTO> serviceOfferings = serviceOfferingService.getAll(pageable);
        return ResponseEntity.ok(serviceOfferings);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOfferingDTO> put(@PathVariable Long id, @Valid @RequestBody ServiceOfferingDTO dto) {
        if (Objects.isNull(serviceOfferingService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        serviceOfferingService.put(id, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<ServiceOfferingDTO> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        if (Objects.isNull(serviceOfferingService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        serviceOfferingService.patch(id, fields);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ServiceOfferingDTO> deleteServiceOffering(@PathVariable Long id) {
        if (serviceOfferingService.isServiceOfferingExistById(id)) {
            serviceOfferingService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }
}
