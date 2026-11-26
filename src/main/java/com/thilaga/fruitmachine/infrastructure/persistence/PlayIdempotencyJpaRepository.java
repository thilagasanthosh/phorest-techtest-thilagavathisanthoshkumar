package com.thilaga.fruitmachine.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlayIdempotencyJpaRepository extends JpaRepository<PlayIdempotencyEntity, UUID> {

  Optional<PlayIdempotencyEntity> findByMachineIdAndIdempotencyKey(UUID machineId, String key);
}
