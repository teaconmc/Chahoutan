package org.teacon.chahoutan.repo;

import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.Correction;

import java.util.UUID;

public interface CorrectionRepository extends CrudRepository<Correction, UUID>
{
    // nothing here
}
