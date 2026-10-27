package br.com.autoshop.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class StockMovementDTO {

    @NotNull(message = "Quantity cannot be empty")
    @Positive(message = "Quantity must be positive")
    @Digits(integer = 16, fraction = 3,
            message = "Quantity must have at most 16 integer digits and 3 fraction digits")
    @Schema(description = "Quantidade positiva na unidade cadastrada, com até 16 dígitos inteiros e 3 casas decimais", example = "2.750")
    private BigDecimal quantity;

    public StockMovementDTO() {
    }
}
