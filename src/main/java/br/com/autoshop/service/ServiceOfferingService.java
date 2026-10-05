package br.com.autoshop.service;

import br.com.autoshop.dto.ServiceOfferingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.model.ServiceOfferingEntity;
import br.com.autoshop.repository.ServiceOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ServiceOfferingService {

    public static final String BASE_COST_IS_INVALID = "BaseCost must be a positive number";
    public static final String ESTIMATED_DURATION_IS_INVALID = "EstimatedDurationMinutes must be a positive integer";
    private final ServiceOfferingRepository serviceOfferingRepository;

    public ServiceOfferingService(ServiceOfferingRepository serviceOfferingRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    public ServiceOfferingDTO getById(Long id) {
        return serviceOfferingRepository.findById(id)
                .map(this::convertToDto)
                .orElse(null);
    }

    public ServiceOfferingDTO getByName(String name) {
        return serviceOfferingRepository.findByName(name)
                .map(this::convertToDto)
                .orElse(null);
    }

    public Long save(ServiceOfferingDTO dto) {
        ServiceOfferingEntity serviceOfferingEntity = builderServiceOfferingEntity(dto).build();
        serviceOfferingRepository.save(serviceOfferingEntity);
        return serviceOfferingEntity.getId();
    }

    public void put(Long id, ServiceOfferingDTO dto) {
        ServiceOfferingEntity serviceOfferingEntity = builderServiceOfferingEntity(dto).id(id).build();
        serviceOfferingRepository.save(serviceOfferingEntity);
    }

    public void patch(Long id, Map<String, Object> fields) throws RequestInvalidException {
        Optional<ServiceOfferingEntity> serviceOfferingOptional = serviceOfferingRepository.findById(id);

        AtomicBoolean isDifferent = new AtomicBoolean(false);
        if (serviceOfferingOptional.isPresent()) {
            ServiceOfferingEntity serviceOffering = serviceOfferingOptional.get();

            fields.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    String converted = String.valueOf(value);

                    switch (key) {
                        case "name" -> {
                            serviceOffering.setName(converted);
                            isDifferent.set(true);
                        }
                        case "description" -> {
                            serviceOffering.setDescription(converted);
                            isDifferent.set(true);
                        }
                        case "category" -> {
                            serviceOffering.setCategory(converted);
                            isDifferent.set(true);
                        }
                        case "baseCost" -> {
                            Double baseCost;
                            try {
                                baseCost = Double.valueOf(converted);
                            } catch (NumberFormatException e) {
                                throw new RequestInvalidException(BASE_COST_IS_INVALID);
                            }
                            if (baseCost <= 0) {
                                throw new RequestInvalidException(BASE_COST_IS_INVALID);
                            }
                            serviceOffering.setBaseCost(baseCost);
                            isDifferent.set(true);
                        }
                        case "estimatedDurationMinutes" -> {
                            Integer duration;
                            try {
                                duration = Integer.valueOf(converted);
                            } catch (NumberFormatException e) {
                                throw new RequestInvalidException(ESTIMATED_DURATION_IS_INVALID);
                            }
                            if (duration <= 0) {
                                throw new RequestInvalidException(ESTIMATED_DURATION_IS_INVALID);
                            }
                            serviceOffering.setEstimatedDurationMinutes(duration);
                            isDifferent.set(true);
                        }
                    }
                }
            });

            if (isDifferent.get()) {
                serviceOffering.setId(id);
                serviceOffering.setUpdateDate(LocalDateTime.now());
                serviceOfferingRepository.save(serviceOffering);
            }
        }
    }

    public void deleteById(Long id) {
        serviceOfferingRepository.updateActiveById(Boolean.FALSE, id);
    }

    public boolean isServiceOfferingExistById(Long id) {
        Optional<ServiceOfferingEntity> serviceOffering = serviceOfferingRepository.getByIdAndActiveIsTrue(id);
        return serviceOffering.isPresent();
    }

    public Page<ServiceOfferingDTO> getAll(Pageable pageable) {
        Page<ServiceOfferingEntity> serviceOfferingEntityPage = serviceOfferingRepository.findAll(pageable);
        return serviceOfferingEntityPage.map(this::convertToDto);
    }

    public ServiceOfferingEntity.ServiceOfferingEntityBuilder builderServiceOfferingEntity(ServiceOfferingDTO dto) {
        return ServiceOfferingEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .baseCost(dto.getBaseCost())
                .estimatedDurationMinutes(dto.getEstimatedDurationMinutes())
                .category(dto.getCategory());
    }

    private ServiceOfferingDTO convertToDto(ServiceOfferingEntity entity) {
        return ServiceOfferingDTO.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .baseCost(entity.getBaseCost())
                .estimatedDurationMinutes(entity.getEstimatedDurationMinutes())
                .category(entity.getCategory())
                .build();
    }

}
