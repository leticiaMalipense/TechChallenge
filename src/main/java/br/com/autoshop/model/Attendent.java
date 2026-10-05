package br.com.autoshop.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "attendent")
@DiscriminatorValue("Attendent")
public class Attendent extends UserEntity {

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String name;

    public Attendent() {

    }
}
