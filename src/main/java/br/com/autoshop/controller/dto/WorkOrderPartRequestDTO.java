package br.com.autoshop.controller.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderPartRequestDTO {

    @NotNull
    private Long partId;

    @NotNull
    @Positive
    @Digits(integer = 16, fraction = 3)
    private BigDecimal quantity;
}
