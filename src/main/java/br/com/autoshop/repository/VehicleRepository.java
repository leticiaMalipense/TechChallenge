package br.com.autoshop.repository;

import br.com.autoshop.repository.model.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {

    Optional<VehicleEntity> findByPlateNumber(String plateNumber);

}
