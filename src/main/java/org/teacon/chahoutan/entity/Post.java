package org.teacon.chahoutan.entity;

import org.teacon.chahoutan.ChahoutanConfig;

import javax.persistence.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_posts")
public class Post
{
    private static final long POST_EPOCH_MILLIS = ChahoutanConfig.POST_EPOCH.toEpochMilli();
    private static final long POST_INTERVAL_MILLIS = ChahoutanConfig.POST_INTERVAL.toMillis();

    @Id
    @Column(name = "id", columnDefinition = "int", nullable = false)
    private Integer id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "revision_id")
    private Revision revision = null;

    @ElementCollection
    @Column(name = "editor", columnDefinition = "varchar(32)", nullable = false)
    @CollectionTable(name = "chahoutan_editors", joinColumns = @JoinColumn(name = "post_id"))
    private Set<String> editor = new HashSet<>();

    public static int getLastPublicPostId(Integer until)
    {
        var millis = Instant.now().minus(ChahoutanConfig.POST_DELAY).toEpochMilli();
        var id = Math.floorDiv(millis - POST_EPOCH_MILLIS, POST_INTERVAL_MILLIS);
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
        var millis = POST_EPOCH_MILLIS + this.getId() * POST_INTERVAL_MILLIS;
        return Instant.ofEpochMilli(millis).atZone(ChahoutanConfig.POST_ZONE_ID).toOffsetDateTime();
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
