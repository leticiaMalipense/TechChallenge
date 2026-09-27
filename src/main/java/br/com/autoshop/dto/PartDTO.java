package br.com.autoshop.dto;

import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PartDTO {

    public static final String INVALID_PRICE_MESSAGE = "must have at most 17 integer digits and 2 fraction digits";

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Code cannot be empty")
    private String code;

    private String description;

    private String category;

    @NotNull(message = "PartType cannot be empty")
    private PartType partType;

    @NotNull(message = "Unit cannot be empty")
    private PartUnit unit;

    @NotNull(message = "CostPrice cannot be empty")
    @PositiveOrZero(message = "CostPrice must be positive or zero")
    @Digits(integer = 17, fraction = 2, message = "CostPrice " + INVALID_PRICE_MESSAGE)
    private BigDecimal costPrice;

    @NotNull(message = "SalePrice cannot be empty")
    @PositiveOrZero(message = "SalePrice must be positive or zero")
    @Digits(integer = 17, fraction = 2, message = "SalePrice " + INVALID_PRICE_MESSAGE)
    private BigDecimal salePrice;

    public PartDTO() {

    }
}