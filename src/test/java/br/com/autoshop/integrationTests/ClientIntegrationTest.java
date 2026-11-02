package br.com.autoshop.integrationTests;

import br.com.autoshop.repository.ClientRepository;
import br.com.autoshop.repository.model.ClientEntity;
import br.com.autoshop.util.DocumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientIntegrationTest extends BaseIntegrationTest {

    private static final String DOCUMENT = "52998224725";

    @Autowired
    private ClientRepository clientRepository;

    private String email;

    @BeforeEach
    void createUniqueEmail() {
        email = "client-" + UUID.randomUUID() + "@example.com";
    }

    @AfterEach
    void removeTestClient() {
        clientRepository.findByDocument(DOCUMENT).ifPresent(clientRepository::delete);
    }

    @Test
    @WithMockUser(roles = "Mechanic")
    void deniesClientEndpointToMechanicRole() throws Exception {
        mockMvc.perform(get("/client/999999999")).andExpect(status().isForbidden());
        assertTrue(clientRepository.findByDocument(DOCUMENT).isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getsClientFromDatabaseThroughHttpEndpoint() throws Exception {
        ClientEntity client = clientRepository.save(ClientEntity.builder()
                .name("Ana Silva").document(DOCUMENT).documentType(DocumentType.CPF)
                .email(email).phone("11999999999").build());

        mockMvc.perform(get("/client/{id}", client.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.document").value(DOCUMENT))
                .andExpect(jsonPath("$.documentType").value("CPF"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.phone").value("11999999999"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createsClientThroughHttpAndPersistsIt() throws Exception {
        mockMvc.perform(post("/client").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson("529.982.247-25", email)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        java.util.regex.Pattern.compile(".*/client/\\d+"))));

        ClientEntity saved = clientRepository.findByDocument(DOCUMENT).orElseThrow();
        assertEquals(email, saved.getEmail());
        assertEquals(DocumentType.CPF, saved.getDocumentType());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidEmailWithoutPersistingClient() throws Exception {
        mockMvc.perform(post("/client").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson(DOCUMENT, "invalid-email")))
                .andExpect(status().isBadRequest());

        assertTrue(clientRepository.findByDocument(DOCUMENT).isEmpty());
    }

    private static String clientJson(String document, String email) {
        return """
                {"name":"Ana Silva","document":"%s","email":"%s","phone":"11999999999"}
                """.formatted(document, email);
    }
}
