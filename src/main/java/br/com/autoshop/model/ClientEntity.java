package br.com.autoshop.model;

import br.com.autoshop.util.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "client")
@AllArgsConstructor
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime creationDate = LocalDateTime.now();

    public ClientEntity() {

    }
}
