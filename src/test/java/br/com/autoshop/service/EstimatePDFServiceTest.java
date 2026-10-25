package br.com.autoshop.service;

import br.com.autoshop.dto.ServiceOfferingDTO;
import br.com.autoshop.dto.WorkOrderResponseDTO;
import br.com.autoshop.util.WorkOrderStatus;
import net.sf.jasperreports.engine.JRException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstimatePDFServiceTest {

    private final EstimatePDFService service = new EstimatePDFService();

    @Test
    void generatePdfReturnsPdfForPopulatedWorkOrder() throws IOException, JRException {
        Path outputPath = Path.of("src", "main", "resources", "reports", "estimate.pdf");
        byte[] previousPdf = Files.exists(outputPath) ? Files.readAllBytes(outputPath) : null;

        WorkOrderResponseDTO workOrder = WorkOrderResponseDTO.builder()
                .id(UUID.randomUUID())
                .clientName("Jane Smith")
                .clientDocument("12345678900")
                .vehicleBrand("Toyota")
                .vehicleModel("Corolla")
                .vehicleYear(2022)
                .vehiclePlateNumber("ABC1D23")
                .status(WorkOrderStatus.AWAITING_APPROVAL)
                .totalEstimate(180.0)
                .services(List.of(ServiceOfferingDTO.builder()
                        .name("Oil change")
                        .description("Oil and filter replacement")
                        .baseCost(180.0)
                        .estimatedDurationMinutes(45)
                        .category("Maintenance")
                        .build()))
                .build();

        try {
            byte[] pdf = service.generatePdf(workOrder);

            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
            assertTrue(new String(pdf, 0, Math.min(pdf.length, 5), StandardCharsets.US_ASCII)
                    .startsWith("%PDF-"));
        } finally {
            if (previousPdf == null) {
                Files.deleteIfExists(outputPath);
            } else {
                Files.write(outputPath, previousPdf);
            }
        }
    }

    @Test
    void generatePdfRejectsNullWorkOrder() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generatePdf(null));

        assertEquals("The work order cannot be null", exception.getMessage());
    }
}
