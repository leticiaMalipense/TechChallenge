package br.com.autoshop.model;

import br.com.autoshop.util.DocumentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "client")
@AllArgsConstructor
@DiscriminatorValue("Client")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @SequenceGenerator(
            sequenceName = "client_id_seq",
            initialValue = 1,
            allocationSize = 1)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true, length = 100, nullable = false)
    @NotNull
    private String email;

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Boolean status = Boolean.TRUE;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    public ClientEntity() {

    }
}
