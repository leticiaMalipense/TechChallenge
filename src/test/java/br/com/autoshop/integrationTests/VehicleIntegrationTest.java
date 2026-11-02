package br.com.autoshop.integrationTests;

import br.com.autoshop.repository.VehicleRepository;
import br.com.autoshop.repository.model.VehicleEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VehicleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    private final Set<String> testPlates = new HashSet<>();
    private String plate;

    @BeforeEach
    void prepareTestPlate() {
        String random = UUID.randomUUID().toString().replace("-", "");
        plate = "TST" + Math.floorMod(random.hashCode(), 10) + "A"
                + String.format("%02d", Math.floorMod(random.hashCode(), 100));
        testPlates.add(plate);
    }

    @AfterEach
    void deleteTestVehicles() {
        testPlates.forEach(testPlate -> vehicleRepository.findByPlateNumber(testPlate)
                .ifPresent(vehicleRepository::delete));
        testPlates.clear();
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    void createsVehicleThroughHttpAndPersistsItInDatabase() throws Exception {
        mockMvc.perform(post("/vehicle").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleJson(plate, "Civic")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/vehicle/" + plate)));

        VehicleEntity saved = vehicleRepository.findByPlateNumber(plate).orElseThrow();
        assertEquals("Honda", saved.getBrand());
        assertEquals("Civic", saved.getModel());
        assertEquals(2022, saved.getYear());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    void rejectsInvalidVehicleAndConflictsOnDuplicatePlate() throws Exception {
        mockMvc.perform(post("/vehicle").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"plateNumber\": \"bad\", \"brand\": \"\", \"model\": \"\", \"year\": null }"))
                .andExpect(status().isBadRequest());
        assertTrue(vehicleRepository.findByPlateNumber(plate).isEmpty());

        persistVehicle(plate, "Civic");
        mockMvc.perform(post("/vehicle").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleJson(plate, "Civic")))
                .andExpect(status().isConflict());
        assertEquals(1, vehicleRepository.findAll().stream()
                .filter(vehicle -> plate.equals(vehicle.getPlateNumber())).count());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "Mechanic")
    void getsAndListsVehiclesFromDatabase() throws Exception {
        persistVehicle(plate, "Civic");

        mockMvc.perform(get("/vehicle/{plate}", plate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value(plate))
                .andExpect(jsonPath("$.brand").value("Honda"));
        mockMvc.perform(get("/vehicle").param("page", "0").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].plateNumber")
                        .value(org.hamcrest.Matchers.hasItem(plate)));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    void returnsNotFoundForUnknownPlate() throws Exception {
        mockMvc.perform(get("/vehicle/{plate}", plate)).andExpect(status().isNotFound());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "Mechanic")
    void updatesAndDeletesVehicleInDatabase() throws Exception {
        persistVehicle(plate, "Civic");

        mockMvc.perform(put("/vehicle/{plate}", plate).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleJson(plate, "Civic")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/vehicle/{plate}", plate).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"model\": \"Accord\" }"))
                .andExpect(status().isOk());
        assertEquals("Accord", vehicleRepository.findByPlateNumber(plate).orElseThrow().getModel());

        mockMvc.perform(delete("/vehicle/{plate}", plate).with(csrf())).andExpect(status().isOk());
        assertTrue(vehicleRepository.findByPlateNumber(plate).isEmpty());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    void returnsNoContentWhenUpdatingOrDeletingUnknownVehicle() throws Exception {
        mockMvc.perform(put("/vehicle/{plate}", plate).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleJson(plate, "Civic")))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/vehicle/{plate}", plate).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"model\": \"Accord\" }"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/vehicle/{plate}", plate).with(csrf())).andExpect(status().isNoContent());
        assertTrue(vehicleRepository.findByPlateNumber(plate).isEmpty());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "CLIENT")
    void deniesVehicleEndpointsToClientRole() throws Exception {
        mockMvc.perform(get("/vehicle")).andExpect(status().isForbidden());
        assertTrue(vehicleRepository.findByPlateNumber(plate).isEmpty());
    }

    private VehicleEntity persistVehicle(String plateNumber, String model) {
        return vehicleRepository.save(VehicleEntity.builder()
                .plateNumber(plateNumber).brand("Honda").model(model).year(2022)
                .lastReview(LocalDateTime.parse("2025-05-12T10:30:00")).build());
    }

    private static String vehicleJson(String plateNumber, String model) {
        return """
                {
                  "plateNumber": "%s",
                  "brand": "Honda",
                  "model": "%s",
                  "year": 2022,
                  "lastReview": "2025-05-12T10:30:00"
                }
                """.formatted(plateNumber, model);
    }
}
