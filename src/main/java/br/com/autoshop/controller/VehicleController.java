package br.com.autoshop.controller;

import br.com.autoshop.dto.VehicleDTO;
import br.com.autoshop.exception.RequestInvalidException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import br.com.autoshop.service.VehicleService;
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

@SecurityRequirement
@RestController
@RequestMapping("/vehicle")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

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

    @GetMapping("/{plateNumber}")
    public ResponseEntity<VehicleDTO> getVehicle(@PathVariable String plateNumber) {
        VehicleDTO vehicle = vehicleService.getByPlateNumber(plateNumber);
        if (Objects.nonNull(vehicle)) {
            return ResponseEntity.ok(vehicle);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Page<VehicleDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "plateNumber") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<VehicleDTO> vehicles = vehicleService.getAll(pageable);
        return ResponseEntity.ok(vehicles);
    }

    @PutMapping("/{plateNumber}")
    public ResponseEntity<VehicleDTO> put(@PathVariable String plateNumber, @Valid @RequestBody VehicleDTO dto) {
        if (!vehicleService.isVehicleExistByPlateNumber(plateNumber)) {
            return ResponseEntity.noContent().build();
        }

        vehicleService.put(plateNumber, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{plateNumber}")
    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<VehicleDTO> patch(@PathVariable String plateNumber, @RequestBody Map<String, Object> fields) {
        if (!vehicleService.isVehicleExistByPlateNumber(plateNumber)) {
            return ResponseEntity.noContent().build();
        }

        vehicleService.patch(plateNumber, fields);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{plateNumber}")
    public ResponseEntity<VehicleDTO> deleteVehicle(@PathVariable String plateNumber) {
        if (vehicleService.isVehicleExistByPlateNumber(plateNumber)) {
            vehicleService.deleteByPlateNumber(plateNumber);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }
}
