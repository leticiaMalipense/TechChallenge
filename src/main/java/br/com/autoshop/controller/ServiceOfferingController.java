package br.com.autoshop.controller;

import br.com.autoshop.controller.dto.ServiceOfferingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.ServiceOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/service-offering")
@PreAuthorize("hasRole('ADMIN')")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @Operation(summary = "Cadastrar serviço", description = "Adiciona um serviço ao catálogo com nome, descrição, custo-base, duração estimada e categoria. Retorna o endereço do recurso criado.")
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

    @Operation(summary = "Consultar serviço por ID", description = "Busca os dados de um serviço do catálogo pelo identificador. Retorna 404 quando não encontrado.")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceOfferingDTO> getServiceOffering(@PathVariable Long id) {
        ServiceOfferingDTO serviceOffering = serviceOfferingService.getById(id);
        if (Objects.nonNull(serviceOffering)) {
            return ResponseEntity.ok(serviceOffering);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Listar serviços", description = "Retorna uma página de serviços do catálogo. Permite informar página, tamanho e campo de ordenação.")
    @GetMapping
    public ResponseEntity<Page<ServiceOfferingDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "name") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<ServiceOfferingDTO> serviceOfferings = serviceOfferingService.getAll(pageable);
        return ResponseEntity.ok(serviceOfferings);
    }

    @Operation(summary = "Substituir dados do serviço", description = "Atualiza os dados do serviço identificado pelo ID usando os campos enviados no corpo da requisição.")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceOfferingDTO> put(@PathVariable Long id, @Valid @RequestBody ServiceOfferingDTO dto) {
        if (Objects.isNull(serviceOfferingService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        serviceOfferingService.put(id, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Atualizar parcialmente serviço", description = "Atualiza os campos enviados para o serviço. Campos aceitos: name, description, category, baseCost e estimatedDurationMinutes.")
    @PatchMapping("/{id}")
    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<ServiceOfferingDTO> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        if (Objects.isNull(serviceOfferingService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        serviceOfferingService.patch(id, fields);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desativar serviço", description = "Desativa o serviço pelo ID para impedir que seja selecionado em novas ordens de serviço. O registro não é removido fisicamente.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ServiceOfferingDTO> deleteServiceOffering(@PathVariable Long id) {
        if (serviceOfferingService.isServiceOfferingExistById(id)) {
            serviceOfferingService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }
}
