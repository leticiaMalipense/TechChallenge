package br.com.autoshop.service;


import br.com.autoshop.controller.dto.ServiceOfferingDTO;
import br.com.autoshop.controller.dto.WorkOrderPartDTO;
import br.com.autoshop.controller.dto.WorkOrderResponseDTO;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EstimatePDFService {

    public byte[] generatePdf(WorkOrderResponseDTO ordem) throws JRException, IOException {
        if (ordem == null) {
            throw new IllegalArgumentException("The work order cannot be null");
        }

        ClassPathResource template = new ClassPathResource("reports/estimate_report_template.jrxml");
        JasperReport report;
        try (InputStream inputStream = template.getInputStream()) {
            report = JasperCompileManager.compileReport(inputStream);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("TITULO", "Service Estimate");
        parameters.put("ORDEM_ID", Objects.toString(ordem.getId(), "Not provided"));
        parameters.put("CLIENTE", textOrDefault(ordem.getClientName()));
        parameters.put("DOCUMENTO", textOrDefault(ordem.getClientDocument()));
        parameters.put("VEICULO", buildVehicleDescription(ordem));
        parameters.put("PLACA", textOrDefault(ordem.getVehiclePlateNumber()));
        parameters.put("STATUS", ordem.getStatus() == null
                ? "Not provided" : ordem.getStatus().toString());
        parameters.put("TOTAL", Objects.requireNonNullElse(ordem.getTotalEstimate(), 0.0));

        // Linhas unificadas (serviços + peças) com os campos name/description/baseCost esperados pelo template.
        List<Map<String, ?>> lines = new ArrayList<>();
        if (ordem.getServices() != null) {
            ordem.getServices().stream()
                    .filter(Objects::nonNull)
                    .map(this::normalizeService)
                    .forEach(s -> lines.add(Map.of(
                            "name", s.getName(),
                            "description", s.getDescription(),
                            "baseCost", s.getBaseCost())));
        }
        if (ordem.getParts() != null) {
            ordem.getParts().stream()
                    .filter(Objects::nonNull)
                    .forEach(p -> lines.add(partLine(p)));
        }
        JasperPrint filledReport = JasperFillManager.fillReport(
                report, parameters, new JRMapCollectionDataSource(lines));


        return JasperExportManager.exportReportToPdf(filledReport);
    }

    private String buildVehicleDescription(WorkOrderResponseDTO workOrder) {
        String brand = textOrDefault(workOrder.getVehicleBrand());
        String model = textOrDefault(workOrder.getVehicleModel());
        String year = workOrder.getVehicleYear() == null ? "Not provided" : workOrder.getVehicleYear().toString();
        return brand + " " + model + " (" + year + ")";
    }

    private Map<String, Object> partLine(WorkOrderPartDTO part) {
        BigDecimal quantity = Objects.requireNonNullElse(part.getQuantity(), BigDecimal.ZERO);
        BigDecimal unitPrice = Objects.requireNonNullElse(part.getUnitPrice(), BigDecimal.ZERO);
        String unit = part.getUnit() == null ? "" : " " + part.getUnit();
        return Map.of(
                "name", textOrDefault(part.getName()),
                "description", quantity.stripTrailingZeros().toPlainString() + unit
                        + " x " + unitPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "baseCost", quantity.multiply(unitPrice).doubleValue());
    }

    private ServiceOfferingDTO normalizeService(ServiceOfferingDTO service) {
        return ServiceOfferingDTO.builder()
                .name(textOrDefault(service.getName()))
                .description(textOrDefault(service.getDescription()))
                .baseCost(Objects.requireNonNullElse(service.getBaseCost(), 0.0))
                .estimatedDurationMinutes(Objects.requireNonNullElse(
                        service.getEstimatedDurationMinutes(), 0))
                .category(textOrDefault(service.getCategory()))
                .build();
    }

    private String textOrDefault(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
