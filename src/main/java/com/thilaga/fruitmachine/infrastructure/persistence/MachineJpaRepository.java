package com.thilaga.fruitmachine.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MachineJpaRepository extends JpaRepository<MachineEntity, UUID> {}
