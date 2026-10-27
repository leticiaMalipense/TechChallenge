package br.com.autoshop.service;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.exception.AlreadyExistsException;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.model.PartEntity;
import br.com.autoshop.repository.PartRepository;
import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public PartDTO getById(Long id) {
        return partRepository.getByIdAndActiveIsTrue(id)
                .map(this::convertToDto)
                .orElse(null);
    }

    public PartDTO getByCode(String code) {
        return partRepository.findByCodeAndActiveTrue(code)
                .map(this::convertToDto)
                .orElse(null);
    }

    @Transactional
    public Long save(PartDTO dto) {
        if (partRepository.existsByCode(dto.getCode())) {
            throw new AlreadyExistsException("Part code already exists");
        }

        PartEntity partEntity = builderPartEntity(dto).build();
        partRepository.save(partEntity);
        return partEntity.getId();
    }

    public void put(Long id, PartDTO dto) {
        Optional<PartEntity> current = partRepository.getByIdAndActiveIsTrue(id);
        if (current.isEmpty()) {
            return;
        }
        PartEntity existing = current.get();
        if (!Objects.equals(existing.getCode(), dto.getCode())
                && partRepository.existsByCode(dto.getCode())) {
            throw new AlreadyExistsException("Part code already exists");
        }

        partRepository.save(builderPartEntity(dto)
                .id(existing.getId())
                .creationDate(existing.getCreationDate())
                .active(existing.getActive())
                .build());
    }

    public void patch(Long id, Map<String, Object> fields) throws RequestInvalidException {
        Optional<PartEntity> partEntityOptional = partRepository.getByIdAndActiveIsTrue(id);
        if (partEntityOptional.isEmpty()) {
            return;
        }

        PartEntity part = partEntityOptional.get();
        AtomicBoolean isDifferent = new AtomicBoolean(false);
        fields.forEach((key, value) -> {
            if (Objects.isNull(value)) {
                return;
            }

            String converted = String.valueOf(value);
            switch (key) {
                case "name" -> {
                    if (converted.isBlank()) {
                        throw new RequestInvalidException("Name cannot be empty");
                    }
                    part.setName(converted);
                    isDifferent.set(true);
                }
                case "description" -> {
                    part.setDescription(converted);
                    isDifferent.set(true);
                }
                case "code" -> {
                    if (converted.isBlank()) {
                        throw new RequestInvalidException("Code cannot be empty");
                    }
                    if (!Objects.equals(part.getCode(), converted) && partRepository.existsByCode(converted)) {
                        throw new AlreadyExistsException("Part code already exists");
                    }
                    part.setCode(converted);
                    isDifferent.set(true);
                }
                case "category" -> {
                    part.setCategory(converted);
                    isDifferent.set(true);
                }
                case "partType" -> {
                    if (!PartType.isValueOf(converted)) {
                        throw new RequestInvalidException("PartType is invalid");
                    }
                    part.setPartType(PartType.valueOf(converted));
                    isDifferent.set(true);
                }
                case "unit" -> {
                    if (!PartUnit.isValueOf(converted)) {
                        throw new RequestInvalidException("PartUnit is invalid");
                    }
                    part.setUnit(PartUnit.valueOf(converted));
                    isDifferent.set(true);
                }
                case "costPrice" -> {
                    part.setCostPrice(parsePrice(converted));
                    isDifferent.set(true);
                }
                case "salePrice" -> {
                    part.setSalePrice(parsePrice(converted));
                    isDifferent.set(true);
                }
                default -> {
                }
            }
        });

        if (isDifferent.get()) {
            partRepository.save(part);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        partRepository.updateActiveById(Boolean.FALSE, id);
    }

    public boolean isPartExistById(Long id) {
        return partRepository.getByIdAndActiveIsTrue(id).isPresent();
    }

    public Page<PartDTO> getAll(Pageable pageable) {
        return partRepository.findByActiveTrue(pageable).map(this::convertToDto);
    }

    private BigDecimal parsePrice(String value) {
        try {
            BigDecimal price = new BigDecimal(value);
            if (price.signum() < 0) {
                throw new RequestInvalidException("Price must be positive or zero");
            }
            if (price.precision() - price.scale() > 17 || price.scale() > 2) {
                throw new RequestInvalidException("Price " + PartDTO.INVALID_PRICE_MESSAGE);
            }
            return price;
        } catch (NumberFormatException exception) {
            throw new RequestInvalidException("Price must be a valid number");
        }
    }

    private PartDTO convertToDto(PartEntity entity) {
        return PartDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .partType(entity.getPartType())
                .unit(entity.getUnit())
                .costPrice(entity.getCostPrice())
                .salePrice(entity.getSalePrice())
                .build();
    }

    private PartEntity.PartEntityBuilder builderPartEntity(PartDTO dto) {
        return PartEntity.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .partType(dto.getPartType())
                .unit(dto.getUnit())
                .costPrice(dto.getCostPrice())
                .salePrice(dto.getSalePrice());
    }

}
