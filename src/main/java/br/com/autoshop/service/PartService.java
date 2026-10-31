package br.com.autoshop.service;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.exception.AlreadyExistsException;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.exception.PartNotFoundException;
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

    @Transactional
    public void put(Long id, PartDTO dto) {
        PartEntity existing = getActivePartForUpdate(id);
        if (!Objects.equals(existing.getCode(), dto.getCode())
                && partRepository.existsByCode(dto.getCode())) {
            throw new AlreadyExistsException("Part code already exists");
        }

        validateUnitChange(existing, dto.getUnit());
        partRepository.save(builderPartEntity(dto)
                .id(existing.getId())
                .creationDate(existing.getCreationDate())
                .active(existing.getActive())
                .stockQuantity(existing.getStockQuantity())
                .build());
    }

    @Transactional
    public void patch(Long id, Map<String, Object> fields) throws RequestInvalidException {
        PartEntity part = getActivePartForUpdate(id);
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
                    validateUnitChange(part, PartUnit.valueOf(converted));
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
        getActivePartForUpdate(id);
        partRepository.updateActiveById(Boolean.FALSE, id);
    }

    public boolean isPartExistById(Long id) {
        return partRepository.getByIdAndActiveIsTrue(id).isPresent();
    }

    public Page<PartDTO> getAll(Pageable pageable) {
        return partRepository.findByActiveTrue(pageable).map(this::convertToDto);
    }

    @Transactional
    public PartDTO addStock(Long id, BigDecimal quantity) {
        validateStockQuantity(quantity);
        Optional<PartEntity> current = partRepository.findActiveByIdForUpdate(id);
        if (current.isEmpty()) {
            return null;
        }

        PartEntity part = current.get();
        BigDecimal balance = part.getStockQuantity().add(quantity);
        if (balance.precision() - balance.scale() > 16) {
            throw new RequestInvalidException("Stock balance exceeds 16 integer digits");
        }
        part.setStockQuantity(balance);
        partRepository.save(part);
        return convertToDto(part);
    }

    @Transactional
    public PartDTO removeStock(Long id, BigDecimal quantity) {
        validateStockQuantity(quantity);
        Optional<PartEntity> current = partRepository.findActiveByIdForUpdate(id);
        if (current.isEmpty()) {
            return null;
        }

        PartEntity part = current.get();
        if (part.getStockQuantity().compareTo(quantity) < 0) {
            throw new RequestInvalidException("Insufficient stock");
        }
        part.setStockQuantity(part.getStockQuantity().subtract(quantity));
        partRepository.save(part);
        return convertToDto(part);
    }

    private PartEntity getActivePartForUpdate(Long id) {
        return partRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new PartNotFoundException("Part not found or inactive"));
    }

    private void validateUnitChange(PartEntity part, PartUnit unit) {
        if (!Objects.equals(part.getUnit(), unit) && part.getStockQuantity().signum() != 0) {
            throw new RequestInvalidException("Unit cannot change while stock balance is not zero");
        }
    }

    private void validateStockQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new RequestInvalidException("Quantity must be positive");
        }
        if (quantity.precision() - quantity.scale() > 16 || quantity.scale() > 3) {
            throw new RequestInvalidException("Quantity must have at most 16 integer digits and 3 fraction digits");
        }
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
                .stockQuantity(entity.getStockQuantity())
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
