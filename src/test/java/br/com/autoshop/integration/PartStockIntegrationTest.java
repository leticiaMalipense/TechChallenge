package br.com.autoshop.integration;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.repository.PartRepository;
import br.com.autoshop.repository.model.PartEntity;
import br.com.autoshop.service.PartService;
import br.com.autoshop.util.PartType;
import br.com.autoshop.util.PartUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PartStockIntegrationTest {

    @Autowired
    private PartService partService;

    @Autowired
    private PartRepository partRepository;

    private Long id;

    @BeforeEach
    void createPart() {
        id = partService.save(PartDTO.builder().name("Stock integration test")
                .code(UUID.randomUUID().toString()).partType(PartType.SUPPLY).unit(PartUnit.LITER)
                .costPrice(BigDecimal.ONE).salePrice(BigDecimal.TEN).build());
    }

    @AfterEach
    void removeTestPart() {
        if (id != null) {
            partRepository.deleteById(id);
        }
    }

    @Test
    void balanceSurvivesCatalogUpdatesAndInactivePartRejectsMovements() {
        assertEquals(0, partService.getById(id).getStockQuantity().signum());
        partService.addStock(id, new BigDecimal("2.750"));
        partService.removeStock(id, new BigDecimal("0.250"));
        PartDTO dto = partService.getById(id);
        dto.setName("Updated stock test");
        dto.setStockQuantity(BigDecimal.ZERO);
        partService.put(id, dto);
        partService.patch(id, Map.of("description", "Updated", "stockQuantity", 999));
        assertEquals(0, new BigDecimal("2.5").compareTo(partService.getById(id).getStockQuantity()));

        partService.deleteById(id);

        assertNull(partService.getById(id));
        assertNull(partService.addStock(id, BigDecimal.ONE));
        assertNull(partService.removeStock(id, BigDecimal.ONE));
        PartEntity persisted = partRepository.findById(id).orElseThrow();
        assertFalse(persisted.getActive());
        assertEquals(0, new BigDecimal("2.5").compareTo(persisted.getStockQuantity()));
    }

    @Test
    void concurrentExitsCannotConsumeSameBalance() throws Exception {
        partService.addStock(id, BigDecimal.TEN);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> withdraw = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start withdrawals");
                }
                try {
                    partService.removeStock(id, new BigDecimal("7"));
                    return true;
                } catch (RequestInvalidException exception) {
                    assertEquals("Insufficient stock", exception.getMessage());
                    return false;
                }
            };
            var first = executor.submit(withdraw);
            var second = executor.submit(withdraw);
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
        assertEquals(0, new BigDecimal("3").compareTo(partService.getById(id).getStockQuantity()));
    }
}
