package br.com.autoshop.service;

import br.com.autoshop.controller.dto.VehicleDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.VehicleRepository;
import br.com.autoshop.repository.model.VehicleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class VehicleService {

    public static final String LAST_REVIEW_IS_INVALID = "LastReview date format is invalid";
    public static final String YEAR_IS_INVALID = "Year format is invalid";

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleDTO getByPlateNumber(String plateNumber) {
        return vehicleRepository.findByPlateNumber(plateNumber)
                .map(this::convertToDto)
                .orElse(null);
    }

    public String save(VehicleDTO dto) {
        VehicleEntity vehicleEntity = builderVehicleEntity(dto).build();
        vehicleRepository.save(vehicleEntity);
        return vehicleEntity.getPlateNumber();
    }

    public void put(String plateNumber, VehicleDTO dto) {
        Optional<VehicleEntity> existing = vehicleRepository.findByPlateNumber(plateNumber);
        if (existing.isPresent()) {
            VehicleEntity vehicleEntity = builderVehicleEntity(dto)
                    .id(existing.get().getId())
                    .creationDate(existing.get().getCreationDate())
                    .build();
            vehicleRepository.save(vehicleEntity);
        } else {
            VehicleEntity vehicleEntity = builderVehicleEntity(dto).build();
            vehicleRepository.save(vehicleEntity);
        }
    }

    public void patch(String plateNumber, Map<String, Object> fields) throws RequestInvalidException {
        Optional<VehicleEntity> vehicleEntityOptional = vehicleRepository.findByPlateNumber(plateNumber);

        AtomicBoolean isDifferent = new AtomicBoolean(false);
        if (vehicleEntityOptional.isPresent()) {
            VehicleEntity vehicle = vehicleEntityOptional.get();

            fields.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    String converted = String.valueOf(value);

                    switch (key) {
                        case "plateNumber" -> {
                            vehicle.setPlateNumber(converted);
                            isDifferent.set(true);
                        }
                        case "brand" -> {
                            vehicle.setBrand(converted);
                            isDifferent.set(true);
                        }
                        case "model" -> {
                            vehicle.setModel(converted);
                            isDifferent.set(true);
                        }
                        case "year" -> {
                            try {
                                vehicle.setYear(Integer.parseInt(converted));
                                isDifferent.set(true);
                            } catch (NumberFormatException e) {
                                throw new RequestInvalidException(YEAR_IS_INVALID);
                            }
                        }
                        case "lastReview" -> {
                            try {
                                vehicle.setLastReview(LocalDateTime.parse(converted));
                                isDifferent.set(true);
                            } catch (DateTimeParseException e) {
                                throw new RequestInvalidException(LAST_REVIEW_IS_INVALID);
                            }
                        }
                    }
                }
            });

            if (isDifferent.get()) {
                vehicleRepository.save(vehicle);
            }
        }
    }

    public void deleteByPlateNumber(String plateNumber) {
        vehicleRepository.findByPlateNumber(plateNumber).ifPresent(vehicleRepository::delete);
    }

    public boolean isVehicleExistByPlateNumber(String plateNumber) {
        return vehicleRepository.findByPlateNumber(plateNumber).isPresent();
    }

    public Page<VehicleDTO> getAll(Pageable pageable) {
        Page<VehicleEntity> vehicleEntityPage = vehicleRepository.findAll(pageable);
        return vehicleEntityPage.map(this::convertToDto);
    }

    public VehicleEntity.VehicleEntityBuilder builderVehicleEntity(VehicleDTO dto) {
        return VehicleEntity.builder()
                .plateNumber(dto.getPlateNumber())
                .brand(dto.getBrand())
                .model(dto.getModel())
                .year(dto.getYear())
                .lastReview(dto.getLastReview());
    }

    private VehicleDTO convertToDto(VehicleEntity entity) {
        return VehicleDTO.builder()
                .plateNumber(entity.getPlateNumber())
                .brand(entity.getBrand())
                .model(entity.getModel())
                .year(entity.getYear())
                .lastReview(entity.getLastReview())
                .build();
    }
}
