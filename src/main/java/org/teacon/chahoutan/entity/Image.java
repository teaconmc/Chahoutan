package org.teacon.chahoutan.entity;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_images")
public class Image
{
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "upload_time", nullable = false)
    private Instant uploadTime = Instant.EPOCH;

    @ElementCollection
    @MapKeyColumn(name = "suffix", length = 16)
    @Column(name = "binary", columnDefinition = "blob", nullable = false)
    @CollectionTable(name = "chahoutan_image_binaries", joinColumns = @JoinColumn(name = "image_id", referencedColumnName = "id"))
    private Map<String, byte[]> binaries = new HashMap<>();

    @ManyToMany
    @OrderBy("creationTime DESC")
    @CollectionTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "image_id", referencedColumnName = "id"))
    private List<Revision> revision = new ArrayList<>();

    public String getId()
    {
        return this.id;
    }

    public Map<String, byte[]> getBinaries()
    {
        return Map.copyOf(this.binaries);
    }

    public List<Revision> getRevisions()
    {
        return List.copyOf(this.revision);
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setUploadTime(Instant uploadTime)
    {
        this.uploadTime = uploadTime;
    }

    public void setBinaries(Map<String, byte[]> binary)
    {
        this.binaries = Map.copyOf(binary);
    }
}
