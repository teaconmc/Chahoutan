package org.teacon.chahoutan.entity;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chahoutan_revisions")
public class Revision
{
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false)
    public UUID id = null;

    @ManyToOne
    @JoinColumn(name = "post", nullable = false)
    public Post post = new Post();

    @Column(name = "creation_time", nullable = false)
    public Instant creationTime = Instant.EPOCH;

    @Lob
    @Column(name = "text", nullable = false)
    public String text = "";

    @ManyToMany
    @OrderColumn(name = "image_ordinal")
    @CollectionTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "revision_id", referencedColumnName = "id"))
    public List<Image> image = new ArrayList<>();

    public static Revision from(Post post, String text)
    {
        var revision = new Revision();
        revision.post = post;
        revision.creationTime = Instant.now();
        revision.text = text;
        return revision;
    }

    public static class Bridge implements ValueBridge<Revision, String>
    {
        @Override
        public String toIndexedValue(Revision value, ValueBridgeToIndexedValueContext context)
        {
            return value == null ? null : value.text;
        }
    }
}
