package br.com.autoshop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "mechanic")
@AllArgsConstructor
@DiscriminatorValue("Client")
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @SequenceGenerator(
            sequenceName = "attendent_id_seq",
            initialValue = 1,
            allocationSize = 1)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    public Mechanic() {
        
    }
}
