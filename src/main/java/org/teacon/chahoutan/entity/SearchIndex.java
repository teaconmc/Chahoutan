package org.teacon.chahoutan.entity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_search_indexes")
public class SearchIndex
{
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post = new Post();

    @Column(name = "search_index_vector", columnDefinition = "tsvector", nullable = false)
    private String searchIndexVector = "";

    public Post getPost()
    {
        return this.post;
    }
}
