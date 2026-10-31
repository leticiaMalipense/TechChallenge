package br.com.autoshop.controller;

import br.com.autoshop.controller.dto.VehicleDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/vehicle")
@Tag(name = "Vehicle", description = "Gerenciamento do cadastro de veículos atendidos pela oficina, incluindo identificação por placa, marca, modelo, ano e histórico de última revisão.")
@PreAuthorize("hasAnyRole('ADMIN', 'Mechanic')")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(
            summary = "Cadastrar um novo veículo",
            description = "Registra um veículo na base da oficina informando placa, marca, modelo, ano e data opcional da última revisão. " +
                    "Existe para permitir o cadastro prévio ou avulso de veículos antes mesmo da abertura de uma Ordem de Serviço, evitando duplicidade de placas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Veículo cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados obrigatórios do veículo ausentes ou inválidos"),
            @ApiResponse(responseCode = "409", description = "Já existe um veículo cadastrado com a placa informada")
    })
    @PostMapping
    public ResponseEntity<Void> postVehicle(@Valid @RequestBody VehicleDTO dto) {
        VehicleDTO vehicle = vehicleService.getByPlateNumber(dto.getPlateNumber());
        if (Objects.nonNull(vehicle)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String plateNumber = vehicleService.save(dto);
        return ResponseEntity.created(getLocation(plateNumber)).build();
    }

    private static @NonNull URI getLocation(String plateNumber) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{plateNumber}")
                .buildAndExpand(plateNumber)
                .toUri();
    }

    @Operation(
            summary = "Buscar veículo pela placa",
            description = "Localiza os dados cadastrais de um veículo específico (marca, modelo, ano e última revisão) a partir da sua placa. " +
                    "Existe para permitir a rápida identificação do veículo na recepção da oficina e verificação do seu histórico de revisão."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículo encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Nenhum veículo encontrado para a placa informada")
    })
    @GetMapping("/{plateNumber}")
    public ResponseEntity<VehicleDTO> getVehicle(@PathVariable String plateNumber) {
        VehicleDTO vehicle = vehicleService.getByPlateNumber(plateNumber);
        if (Objects.nonNull(vehicle)) {
            return ResponseEntity.ok(vehicle);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Listar todos os veículos cadastrados (paginado)",
            description = "Retorna uma lista paginada e ordenada de todos os veículos registrados no sistema. " +
                    "Existe para apoiar consultas administrativas e controle da frota de veículos já atendidos pela oficina."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de veículos retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Page<VehicleDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue = "plateNumber") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<VehicleDTO> vehicles = vehicleService.getAll(pageable);
        return ResponseEntity.ok(vehicles);
    }

    @Operation(
            summary = "Atualizar todos os dados de um veículo",
            description = "Substitui integralmente os dados cadastrais de um veículo existente identificado pela placa. " +
                    "Existe para permitir o recadastramento completo de informações do veículo (como correção conjunta de marca, modelo, ano e revisão)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículo atualizado com sucesso"),
            @ApiResponse(responseCode = "204", description = "Veículo não encontrado para a placa informada"),
            @ApiResponse(responseCode = "400", description = "Dados enviados no corpo da requisição são inválidos")
    })
    @PutMapping("/{plateNumber}")
    public ResponseEntity<VehicleDTO> put(@PathVariable String plateNumber, @Valid @RequestBody VehicleDTO dto) {
        if (!vehicleService.isVehicleExistByPlateNumber(plateNumber)) {
            return ResponseEntity.noContent().build();
        }

        vehicleService.put(plateNumber, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Atualizar parcialmente os dados de um veículo",
            description = "Permite alterar pontualmente apenas campos específicos do veículo (ex: atualizar somente a marca, o modelo, o ano ou a data da última revisão). " +
                    "Existe para facilitar correções rápidas de cadastro sem a necessidade de reenviar todo o objeto do veículo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Campos do veículo atualizados com sucesso"),
            @ApiResponse(responseCode = "204", description = "Veículo não encontrado para a placa informada"),
            @ApiResponse(responseCode = "400", description = "Formato inválido em algum dos campos enviados (ex: ano ou data de revisão)")
    })
    @PatchMapping("/{plateNumber}")
    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<VehicleDTO> patch(@PathVariable String plateNumber, @RequestBody Map<String, Object> fields) {
        if (!vehicleService.isVehicleExistByPlateNumber(plateNumber)) {
            return ResponseEntity.noContent().build();
        }

        vehicleService.patch(plateNumber, fields);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Remover um veículo pela placa",
            description = "Exclui o registro de um veículo da base de dados a partir da sua placa. " +
                    "Existe para permitir a limpeza de cadastros criados incorretamente ou que não devem mais permanecer no sistema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículo removido com sucesso"),
            @ApiResponse(responseCode = "204", description = "Veículo não encontrado para a placa informada")
    })
    @DeleteMapping("/{plateNumber}")
    public ResponseEntity<VehicleDTO> deleteVehicle(@PathVariable String plateNumber) {
        if (vehicleService.isVehicleExistByPlateNumber(plateNumber)) {
            vehicleService.deleteByPlateNumber(plateNumber);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }
}
