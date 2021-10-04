package org.teacon.chahoutan.repo;

import org.springframework.data.repository.CrudRepository;
import org.teacon.chahoutan.entity.Image;

public interface ImageRepository extends CrudRepository<Image, String>
{
}
