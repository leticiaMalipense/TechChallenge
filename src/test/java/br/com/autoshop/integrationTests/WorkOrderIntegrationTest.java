package br.com.autoshop.integrationTests;

import br.com.autoshop.repository.*;
import br.com.autoshop.repository.model.ClientEntity;
import br.com.autoshop.repository.model.ServiceOfferingEntity;
import br.com.autoshop.repository.model.VehicleEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.service.QuoteEmailService;
import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkOrderIntegrationTest extends BaseIntegrationTest {

    private static final String CLIENT_DOCUMENT = "52998224725";

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;
    @Autowired
    private WorkOrderRepository workOrderRepository;
    @Autowired
    private WorkOrderApprovalTokenRepository approvalTokenRepository;
    @Autowired
    private WorkOrderEmailDeliveryRepository emailDeliveryRepository;

    // Email is an external boundary; business services and all database repositories stay real.
    @MockitoBean
    private QuoteEmailService quoteEmailService;

    private ClientEntity client;
    private VehicleEntity vehicle;
    private ServiceOfferingEntity offering;
    private final Set<UUID> createdWorkOrderIds = new HashSet<>();

    @BeforeEach
    void createDatabaseFixtures() {
        String suffix = UUID.randomUUID().toString();
        client = clientRepository.save(ClientEntity.builder()
                .name("Integration client").document(CLIENT_DOCUMENT).documentType(DocumentType.CPF)
                .email("work-order-" + suffix + "@example.com").phone("11999999999").build());
        vehicle = vehicleRepository.save(VehicleEntity.builder()
                .plateNumber(validPlate(suffix)).brand("Honda").model("Civic").year(2022).build());
        offering = serviceOfferingRepository.save(ServiceOfferingEntity.builder()
                .name("Integration service " + suffix).description("Oil change")
                .baseCost(150.0).estimatedDurationMinutes(30).category("Maintenance").build());
    }

    @AfterEach
    void deleteDatabaseFixtures() {
        emailDeliveryRepository.deleteAll(emailDeliveryRepository.findAll().stream()
                .filter(delivery -> createdWorkOrderIds.contains(delivery.getWorkOrder().getId())).toList());
        approvalTokenRepository.deleteAll(approvalTokenRepository.findAll().stream()
                .filter(token -> createdWorkOrderIds.contains(token.getWorkOrder().getId())).toList());
        workOrderRepository.deleteAllById(createdWorkOrderIds);
        if (offering != null) serviceOfferingRepository.deleteById(offering.getId());
        if (vehicle != null) vehicleRepository.deleteById(vehicle.getId());
        if (client != null) clientRepository.deleteById(client.getId());
        createdWorkOrderIds.clear();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createsWorkOrderThroughHttpAndPersistsIt() throws Exception {
        UUID id = createWorkOrder(false);

        mockMvc.perform(get("/work-orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        assertEquals(WorkOrderStatus.RECEIVED, workOrderRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsWorkOrderWithoutRequiredFields() throws Exception {
        mockMvc.perform(post("/work-orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clientDocument\": \"\", \"vehiclePlateNumber\": \"\" }"))
                .andExpect(status().isBadRequest());
        assertTrue(workOrderRepository.findAll().isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getsWorkOrderByIdAndReturnsNotFoundWhenMissing() throws Exception {
        UUID id = createWorkOrder(false);

        mockMvc.perform(get("/work-orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        mockMvc.perform(get("/work-orders/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "Mechanic")
    void listsPersistedWorkOrdersByClientAndPaginatesResults() throws Exception {
        UUID id = createWorkOrder(false);

        mockMvc.perform(get("/work-orders/client/{document}", CLIENT_DOCUMENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
        mockMvc.perform(get("/work-orders").param("page", "0").param("size", "50").param("sortBy", "status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItem(id.toString())));
    }

    @Test
    @WithMockUser(roles = "Mechanic")
    void executesWorkOrderLifecycleAndPersistsEachTransition() throws Exception {
        UUID id = createWorkOrder(false);

        mockMvc.perform(patch("/work-orders/{id}/start-diagnosis", id).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"));
        mockMvc.perform(post("/work-orders/{id}/services", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("[" + offering.getId() + "]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"));
        mockMvc.perform(patch("/work-orders/{id}/send-for-approval", id).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
        mockMvc.perform(post("/work-orders/{id}/estimate-email/resend", id).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/work-orders/{id}/approve", id).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_EXECUTION"));
        mockMvc.perform(patch("/work-orders/{id}/finish", id).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINISHED"));
        mockMvc.perform(patch("/work-orders/{id}/deliver", id).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));

        assertEquals(WorkOrderStatus.DELIVERED, workOrderRepository.findById(id).orElseThrow().getStatus());
        assertNotNull(vehicleRepository.findById(vehicle.getId()).orElseThrow().getLastReview());
    }

    @Test
    @WithMockUser(roles = "Mechanic")
    void persistsAndReturnsLatestEmailDelivery() throws Exception {
        UUID id = createWorkOrder(true);
        mockMvc.perform(patch("/work-orders/{id}/send-for-approval", id).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/work-orders/{id}/estimate-email/latest", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.attemptNumber").value(1));
        assertEquals(1, emailDeliveryRepository.countByWorkOrder_Id(id));

        UUID noAttemptId = createWorkOrder(false);
        mockMvc.perform(get("/work-orders/{id}/estimate-email/latest", noAttemptId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "Mechanic")
    void trackingReadsPersistedApprovedWorkOrderAndRequiresDocumentPrefix() throws Exception {
        UUID id = createWorkOrder(true);
        WorkOrderEntity persisted = workOrderRepository.findById(id).orElseThrow();
        persisted.setStatus(WorkOrderStatus.IN_EXECUTION);
        workOrderRepository.save(persisted);

        mockMvc.perform(get("/work-orders/{id}/tracking", id).param("documentPrefix", "529"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("IN_EXECUTION"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void deniesInternalWorkOrderEndpointsToClientRole() throws Exception {
        mockMvc.perform(get("/work-orders")).andExpect(status().isForbidden());
        assertTrue(createdWorkOrderIds.isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mapsInvalidTransitionAndEmailDeliveryFailureToHttpErrors() throws Exception {
        UUID id = createWorkOrder(true);
        mockMvc.perform(patch("/work-orders/{id}/start-diagnosis", id).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        doThrow(new MailSendException("SMTP unavailable"))
                .when(quoteEmailService).sendQuote(anyString(), any(), anyString(), anyString());
        mockMvc.perform(patch("/work-orders/{id}/send-for-approval", id).with(csrf()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.deliveryStatus").value("FAILED"))
                .andExpect(jsonPath("$.workOrderStatus").value("AWAITING_APPROVAL"));
    }

    private UUID createWorkOrder(boolean includeService) throws Exception {
        String serviceIds = includeService ? "[" + offering.getId() + "]" : "[]";
        String body = """
                {"clientDocument":"%s","vehiclePlateNumber":"%s","serviceIds":%s}
                """.formatted(CLIENT_DOCUMENT, vehicle.getPlateNumber(), serviceIds);
        String response = mockMvc.perform(post("/work-orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/work-orders/")))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        createdWorkOrderIds.add(id);
        return id;
    }

    private static String validPlate(String value) {
        int hash = Math.abs(value.hashCode());
        return "TST" + (hash % 10) + "A" + String.format("%02d", hash % 100);
    }
}
