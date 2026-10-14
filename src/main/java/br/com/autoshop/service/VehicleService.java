package br.com.autoshop.service;

import br.com.autoshop.dto.VehicleDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.model.VehicleEntity;
import br.com.autoshop.repository.VehicleRepository;
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
                .lastReview(dto.getLastReview());
    }

    private VehicleDTO convertToDto(VehicleEntity entity) {
        return VehicleDTO.builder()
                .plateNumber(entity.getPlateNumber())
                .lastReview(entity.getLastReview())
                .build();
    }
}
