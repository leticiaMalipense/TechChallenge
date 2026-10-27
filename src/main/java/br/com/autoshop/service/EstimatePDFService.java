package br.com.autoshop.service;


import br.com.autoshop.controller.dto.ServiceOfferingDTO;
import br.com.autoshop.controller.dto.WorkOrderResponseDTO;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        List<ServiceOfferingDTO> services = ordem.getServices() == null
                ? List.of()
                : ordem.getServices().stream()
                .filter(Objects::nonNull)
                .map(this::normalizeService)
                .toList();
        JasperPrint filledReport = JasperFillManager.fillReport(
                report, parameters, new JRBeanCollectionDataSource(services));


        byte[] document = JasperExportManager.exportReportToPdf(filledReport);
        Path path = Paths.get("src", "main", "resources", "reports")
                .resolve("estimate.pdf");

        Files.write(path, document);
        return document;
    }

    private String buildVehicleDescription(WorkOrderResponseDTO workOrder) {
        String brand = textOrDefault(workOrder.getVehicleBrand());
        String model = textOrDefault(workOrder.getVehicleModel());
        String year = workOrder.getVehicleYear() == null ? "Not provided" : workOrder.getVehicleYear().toString();
        return brand + " " + model + " (" + year + ")";
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
