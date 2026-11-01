package br.com.autoshop.controller.dto;

import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
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
public class WorkOrderPartDTO {

    private Long partId;

    private String name;

    private String code;

    private PartType partType;

    private PartUnit unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}
