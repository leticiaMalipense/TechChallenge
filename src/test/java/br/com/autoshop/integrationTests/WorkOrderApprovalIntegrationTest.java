package br.com.autoshop.integrationTests;

import br.com.autoshop.repository.ClientRepository;
import br.com.autoshop.repository.VehicleRepository;
import br.com.autoshop.repository.WorkOrderApprovalTokenRepository;
import br.com.autoshop.repository.WorkOrderRepository;
import br.com.autoshop.repository.model.ClientEntity;
import br.com.autoshop.repository.model.VehicleEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.service.WorkOrderApprovalTokenService;
import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.WorkOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkOrderApprovalIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WorkOrderApprovalTokenService approvalTokenService;

    @Autowired
    private WorkOrderApprovalTokenRepository tokenRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private ClientEntity client;
    private VehicleEntity vehicle;
    private WorkOrderEntity workOrder;
    private String token;

    @BeforeEach
    void createPendingApprovalInDatabase() {
        String suffix = UUID.randomUUID().toString();
        client = clientRepository.save(ClientEntity.builder()
                .name("Approval integration client").document("52998224725")
                .documentType(DocumentType.CPF).email("approval-" + suffix + "@example.com")
                .phone("11999999999").build());
        vehicle = vehicleRepository.save(VehicleEntity.builder()
                .plateNumber("TST" + Math.floorMod(suffix.hashCode(), 10) + "A"
                        + String.format("%02d", Math.floorMod(suffix.hashCode(), 100)))
                .brand("Honda").model("Civic").year(2022).build());
        workOrder = workOrderRepository.save(WorkOrderEntity.builder()
                .client(client).vehicle(vehicle).status(WorkOrderStatus.AWAITING_APPROVAL).build());
        token = approvalTokenService.issueToken(workOrder.getId());
    }

    @AfterEach
    void deleteApprovalFixture() {
        if (workOrder != null) {
            tokenRepository.deleteAll(tokenRepository.findAll().stream()
                    .filter(savedToken -> savedToken.getWorkOrder().getId().equals(workOrder.getId())).toList());
            workOrderRepository.deleteById(workOrder.getId());
        }
        if (vehicle != null) vehicleRepository.deleteById(vehicle.getId());
        if (client != null) clientRepository.deleteById(client.getId());
    }

    @Test
    void publicGetShowsConfirmationWithoutApproving() throws Exception {
        mockMvc.perform(get("/work-order-approvals/{token}", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(token)))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        assertTrue(approvalTokenService.isValid(token));
        assertEquals(WorkOrderStatus.AWAITING_APPROVAL,
                workOrderRepository.findById(workOrder.getId()).orElseThrow().getStatus());
    }

    @Test
    void invalidGetReturnsGonePage() throws Exception {
        mockMvc.perform(get("/work-order-approvals/invalid-token"))
                .andExpect(status().isGone())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inválido ou expirou")));
    }

    @Test
    void publicPostConfirmsApprovalAndPersistsTransition() throws Exception {
        mockMvc.perform(post("/work-order-approvals/{token}", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Orçamento aprovado")));

        assertEquals(WorkOrderStatus.IN_EXECUTION,
                workOrderRepository.findById(workOrder.getId()).orElseThrow().getStatus());
        assertFalse(approvalTokenService.isValid(token));
    }

    @Test
    void postWithInvalidTokenReturnsGonePage() throws Exception {
        mockMvc.perform(post("/work-order-approvals/invalid-token"))
                .andExpect(status().isGone())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inválido ou expirou")));
    }

    @Test
    void postWhenWorkOrderWasAlreadyProcessedReturnsConflictPage() throws Exception {
        workOrder.setStatus(WorkOrderStatus.IN_EXECUTION);
        workOrderRepository.save(workOrder);

        mockMvc.perform(post("/work-order-approvals/{token}", token))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "não está mais aguardando aprovação")));

        assertEquals(WorkOrderStatus.IN_EXECUTION,
                workOrderRepository.findById(workOrder.getId()).orElseThrow().getStatus());
    }
}
