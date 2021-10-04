package org.teacon.chahoutan.repo;

import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends CrudRepository<Post, Integer>
{
    List<Post> findFirst20PostsByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(Integer id);

    Optional<Post> findByIdAndRevisionNotNull(Integer id);
}

