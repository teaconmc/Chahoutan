package org.teacon.chahoutan.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends CrudRepository<Post, Integer>
{
    List<Post> findByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(Integer id, Pageable pageable);

    Optional<Post> findByIdAndRevisionNotNull(Integer id);
}

