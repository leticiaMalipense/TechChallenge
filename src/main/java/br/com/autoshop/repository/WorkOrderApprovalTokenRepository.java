package br.com.autoshop.repository;

import br.com.autoshop.repository.model.WorkOrderApprovalTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderApprovalTokenRepository extends JpaRepository<WorkOrderApprovalTokenEntity, UUID> {

    Optional<WorkOrderApprovalTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update WorkOrderApprovalTokenEntity token
            set token.revokedAt = :now
            where token.workOrder.id = :workOrderId
              and token.usedAt is null
              and token.revokedAt is null
            """)
    int revokeActiveTokens(@Param("workOrderId") UUID workOrderId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update WorkOrderApprovalTokenEntity token
            set token.usedAt = :now
            where token.tokenHash = :tokenHash
              and token.usedAt is null
              and token.revokedAt is null
              and token.expiresAt > :now
            """)
    int consumeToken(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update WorkOrderApprovalTokenEntity token
            set token.revokedAt = :now
            where token.workOrder.id = :workOrderId
              and token.usedAt is null
              and token.revokedAt is null
            """)
    int revokeUnconsumedTokens(@Param("workOrderId") UUID workOrderId, @Param("now") LocalDateTime now);
}
