package org.teacon.chahoutan.repo;

import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.Metadata;

import java.util.List;

public interface MetadataRepository extends CrudRepository<Metadata, String>
{
    List<Metadata> findAllByOrderByIdAsc();
}
