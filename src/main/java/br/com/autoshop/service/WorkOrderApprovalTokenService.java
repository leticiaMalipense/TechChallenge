package br.com.autoshop.service;

import br.com.autoshop.exception.ApprovalConflictException;
import br.com.autoshop.exception.InvalidApprovalTokenException;
import br.com.autoshop.repository.WorkOrderApprovalTokenRepository;
import br.com.autoshop.repository.WorkOrderRepository;
import br.com.autoshop.repository.model.WorkOrderApprovalTokenEntity;
import br.com.autoshop.repository.model.WorkOrderEntity;
import br.com.autoshop.util.WorkOrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class WorkOrderApprovalTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WorkOrderApprovalTokenRepository tokenRepository;
    private final WorkOrderRepository workOrderRepository;
    private final long tokenTtlHours;

    public WorkOrderApprovalTokenService(
            WorkOrderApprovalTokenRepository tokenRepository,
            WorkOrderRepository workOrderRepository,
            @Value("${autoshop.approval.token-ttl-hours:72}") long tokenTtlHours
    ) {
        if (tokenTtlHours < 1) {
            throw new IllegalArgumentException("Approval token lifetime must be at least one hour");
        }
        this.tokenRepository = tokenRepository;
        this.workOrderRepository = workOrderRepository;
        this.tokenTtlHours = tokenTtlHours;
    }

    @Transactional
    public String issueToken(UUID workOrderId) {
        WorkOrderEntity workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found"));
        if (workOrder.getStatus() != WorkOrderStatus.AWAITING_APPROVAL) {
            throw new ApprovalConflictException();
        }
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.revokeActiveTokens(workOrderId, now);

        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        tokenRepository.save(WorkOrderApprovalTokenEntity.builder()
                .workOrder(workOrder)
                .tokenHash(hashToken(rawToken))
                .createdAt(now)
                .expiresAt(now.plusHours(tokenTtlHours))
                .build());
        return rawToken;
    }

    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findByTokenHash(hashToken(rawToken))
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(now))
                .filter(token -> token.getWorkOrder().getStatus() == WorkOrderStatus.AWAITING_APPROVAL)
                .isPresent();
    }

    @Transactional
    public void revokeTokens(UUID workOrderId) {
        tokenRepository.revokeActiveTokens(workOrderId, LocalDateTime.now());
    }

    @Transactional
    public void approve(String rawToken) {
        String tokenHash = hashToken(rawToken);
        LocalDateTime now = LocalDateTime.now();
        WorkOrderApprovalTokenEntity token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidApprovalTokenException::new);

        if (token.getUsedAt() != null || token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidApprovalTokenException();
        }

        int consumed = tokenRepository.consumeToken(tokenHash, now);
        if (consumed != 1) {
            throw new InvalidApprovalTokenException();
        }

        int transitioned = workOrderRepository.transitionStatus(
                token.getWorkOrder().getId(),
                WorkOrderStatus.AWAITING_APPROVAL,
                WorkOrderStatus.IN_EXECUTION,
                now
        );
        if (transitioned != 1) {
            throw new ApprovalConflictException();
        }

        tokenRepository.revokeUnconsumedTokens(token.getWorkOrder().getId(), now);
    }

    private static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidApprovalTokenException();
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
