package br.com.autoshop.model;

import br.com.autoshop.util.HoleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "users")
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true, length = 100, nullable = false)
    @NotNull
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "role", insertable = false, updatable = false)
    private HoleType role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.FALSE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedDate = LocalDateTime.now();

    public UserEntity() {

    }
}