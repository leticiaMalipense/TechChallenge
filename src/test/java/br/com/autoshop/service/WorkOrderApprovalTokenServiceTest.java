package br.com.autoshop.service;

import br.com.autoshop.exception.ApprovalConflictException;
import br.com.autoshop.exception.InvalidApprovalTokenException;
import br.com.autoshop.repository.WorkOrderApprovalTokenRepository;
import br.com.autoshop.repository.WorkOrderRepository;
import br.com.autoshop.repository.model.WorkOrderApprovalTokenEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.util.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderApprovalTokenServiceTest {

    @Mock
    private WorkOrderApprovalTokenRepository tokenRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;

    private WorkOrderApprovalTokenService service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderApprovalTokenService(tokenRepository, workOrderRepository, 72);
    }

    @Test
    void issuesRandomTokenAndPersistsOnlyItsHash() throws Exception {
        UUID id = UUID.randomUUID();
        WorkOrderEntity order = WorkOrderEntity.builder().id(id).status(WorkOrderStatus.AWAITING_APPROVAL).build();
        when(workOrderRepository.findById(id)).thenReturn(Optional.of(order));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = service.issueToken(id);

        assertEquals(43, rawToken.length());
        ArgumentCaptor<WorkOrderApprovalTokenEntity> token =
                ArgumentCaptor.forClass(WorkOrderApprovalTokenEntity.class);
        verify(tokenRepository).save(token.capture());
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expectedHash, token.getValue().getTokenHash());
        assertNotEquals(rawToken, token.getValue().getTokenHash());
        assertTrue(token.getValue().getExpiresAt().isAfter(token.getValue().getCreatedAt()));
    }

    @Test
    void approvalConsumesTokenAndTransitionsOnlyAwaitingWorkOrder() {
        UUID id = UUID.randomUUID();
        WorkOrderEntity order = WorkOrderEntity.builder().id(id).status(WorkOrderStatus.AWAITING_APPROVAL).build();
        WorkOrderApprovalTokenEntity token = WorkOrderApprovalTokenEntity.builder()
                .workOrder(order)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(tokenRepository.consumeToken(anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(workOrderRepository.transitionStatus(
                eq(id),
                eq(WorkOrderStatus.AWAITING_APPROVAL),
                eq(WorkOrderStatus.IN_EXECUTION),
                any(LocalDateTime.class)
        )).thenReturn(1);

        service.approve("valid-token");

        verify(tokenRepository).consumeToken(anyString(), any(LocalDateTime.class));
        verify(workOrderRepository).transitionStatus(
                eq(id),
                eq(WorkOrderStatus.AWAITING_APPROVAL),
                eq(WorkOrderStatus.IN_EXECUTION),
                any(LocalDateTime.class)
        );
    }

    @Test
    void rejectsTokenWhenWorkOrderWasAlreadyProcessed() {
        WorkOrderEntity order = WorkOrderEntity.builder()
                .id(UUID.randomUUID())
                .status(WorkOrderStatus.IN_EXECUTION)
                .build();
        WorkOrderApprovalTokenEntity token = WorkOrderApprovalTokenEntity.builder()
                .workOrder(order)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(tokenRepository.consumeToken(anyString(), any(LocalDateTime.class))).thenReturn(1);
        when(workOrderRepository.transitionStatus(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(0);

        assertThrows(ApprovalConflictException.class, () -> service.approve("valid-token"));
    }

    @Test
    void rejectsExpiredTokenBeforeChangingWorkOrder() {
        WorkOrderApprovalTokenEntity token = WorkOrderApprovalTokenEntity.builder()
                .workOrder(WorkOrderEntity.builder().id(UUID.randomUUID()).build())
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(InvalidApprovalTokenException.class, () -> service.approve("expired-token"));
        verify(tokenRepository, never()).consumeToken(anyString(), any());
        verify(workOrderRepository, never()).transitionStatus(any(), any(), any(), any());
    }
}
