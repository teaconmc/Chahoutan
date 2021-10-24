package org.teacon.chahoutan.entity;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import javax.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_posts")
@Indexed(index = "chahoutan-indexes")
public class Post
{
    private static final Duration POST_DELAY = Duration.ofMillis(100_800_000L);

    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(+8);

    private static final long POST_EPOCH = 1_611_920_400_000L;

    private static final long POST_INTERVAL = 201_600_000L;

    @Id
    @DocumentId
    @GenericField(name = "id")
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "revision_id")
    @FullTextField(name = "text", analyzer = "smartcn", valueBridge = @ValueBridgeRef(type = Revision.Bridge.class))
    private Revision revision = null;

    @ElementCollection
    @KeywordField(name = "editor")
    @Column(columnDefinition = "text", nullable = false)
    @CollectionTable(name = "chahoutan_editors")
    private Set<String> editor = new HashSet<>();

    public static int getLastPublicPostId(Integer until)
    {
        var id = Math.floorDiv(Instant.now().minus(POST_DELAY).toEpochMilli() - POST_EPOCH, POST_INTERVAL);
        return Math.toIntExact(until == null ? id : Math.min(id, until));
    }

    public Integer getId()
    {
        return this.id;
    }

    public Revision getRevision()
    {
        return this.revision;
    }

    public List<String> getEditors()
    {
        return this.editor.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public OffsetDateTime getPublishTime()
    {
        return Instant.ofEpochMilli(POST_EPOCH + this.getId() * POST_INTERVAL).atOffset(ZONE_OFFSET);
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public void setRevision(Revision revision)
    {
        this.revision = revision;
    }

    public void setEditors(List<String> editors)
    {
        this.editor = new HashSet<>(editors);
    }
}
