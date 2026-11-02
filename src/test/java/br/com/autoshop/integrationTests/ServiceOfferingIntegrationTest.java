package br.com.autoshop.integrationTests;

import br.com.autoshop.repository.ServiceOfferingRepository;
import br.com.autoshop.repository.model.ServiceOfferingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServiceOfferingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    private String name;

    @BeforeEach
    void createUniqueName() {
        name = "Integration service " + UUID.randomUUID();
    }

    @AfterEach
    void removeCreatedOffering() {
        serviceOfferingRepository.findByName(name).ifPresent(serviceOfferingRepository::delete);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createsServiceOfferingThroughHttpAndPersistsIt() throws Exception {
        mockMvc.perform(post("/service-offering").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(serviceJson(name, 150.0)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        java.util.regex.Pattern.compile(".*/service-offering/\\d+"))));

        ServiceOfferingEntity persisted = serviceOfferingRepository.findByName(name).orElseThrow();
        assertEquals(150.0, persisted.getBaseCost());
        assertTrue(persisted.getActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidInputAndConflictsOnExistingOffering() throws Exception {
        mockMvc.perform(post("/service-offering").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\", \"description\": \"\", \"baseCost\": 0, \"estimatedDurationMinutes\": 0, \"category\": \"\" }"))
                .andExpect(status().isBadRequest());

        persistOffering(name);
        mockMvc.perform(post("/service-offering").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(serviceJson(name, 150.0)))
                .andExpect(status().isConflict());
        assertEquals(1, serviceOfferingRepository.findAll().stream()
                .filter(offering -> name.equals(offering.getName())).count());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getsAndListsServiceOfferingsFromDatabase() throws Exception {
        ServiceOfferingEntity saved = persistOffering(name);

        mockMvc.perform(get("/service-offering/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.baseCost").value(150.0));
        mockMvc.perform(get("/service-offering").param("page", "0").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(org.hamcrest.Matchers.hasItem(name)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsNotFoundForMissingOffering() throws Exception {
        mockMvc.perform(get("/service-offering/999999999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatesAndDeactivatesOfferingInDatabase() throws Exception {
        ServiceOfferingEntity saved = persistOffering(name);

        mockMvc.perform(put("/service-offering/{id}", saved.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(serviceJson(name, 180.0)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/service-offering/{id}", saved.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"category\": \"Revisão\" }"))
                .andExpect(status().isOk());
        ServiceOfferingEntity changed = serviceOfferingRepository.findById(saved.getId()).orElseThrow();
        assertEquals(180.0, changed.getBaseCost());
        assertEquals("Revisão", changed.getCategory());

        mockMvc.perform(delete("/service-offering/{id}", saved.getId()).with(csrf()))
                .andExpect(status().isOk());
        assertFalse(serviceOfferingRepository.findById(saved.getId()).orElseThrow().getActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsNoContentForMissingOfferingUpdatesAndDelete() throws Exception {
        mockMvc.perform(put("/service-offering/999999999").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(serviceJson(name, 150.0)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/service-offering/999999999").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"Novo nome\" }"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/service-offering/999999999").with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(serviceOfferingRepository.findByName(name).isEmpty());
    }

    @Test
    @WithMockUser(roles = "Mechanic")
    void deniesOfferingEndpointsToNonAdmin() throws Exception {
        mockMvc.perform(get("/service-offering")).andExpect(status().isForbidden());
    }

    private ServiceOfferingEntity persistOffering(String offeringName) {
        return serviceOfferingRepository.save(ServiceOfferingEntity.builder()
                .name(offeringName).description("Troca de óleo do motor").baseCost(150.0)
                .estimatedDurationMinutes(30).category("Manutenção").build());
    }

    private static String serviceJson(String offeringName, double baseCost) {
        return """
                {"name":"%s","description":"Troca de óleo do motor","baseCost":%s,
                 "estimatedDurationMinutes":30,"category":"Manutenção"}
                """.formatted(offeringName, baseCost);
    }
}
