package org.teacon.chahoutan.repo;

import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;

import java.util.List;
import java.util.UUID;

public interface RevisionRepository extends CrudRepository<Revision, UUID>
{
    List<Revision> findRevisionsByPostOrderByCreationTimeDesc(Post post);
}
