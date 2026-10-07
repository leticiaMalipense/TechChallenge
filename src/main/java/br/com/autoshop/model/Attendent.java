package br.com.autoshop.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "attendent")
@DiscriminatorValue("Attendent")
public class Attendent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @SequenceGenerator(
            sequenceName = "attendent_id_seq",
            initialValue = 1,
            allocationSize = 1)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String name;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    public Attendent() {

    }
}
