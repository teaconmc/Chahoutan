package org.teacon.chahoutan.entity;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
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
    @CollectionTable(name = "chahoutan_image_binaries", joinColumns = @JoinColumn(name = "image_id", referencedColumnName = "id"))
    private Map<String, Binary> binary = new HashMap<>();

    @ManyToMany
    @OrderBy("creationTime DESC")
    @CollectionTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "image_id", referencedColumnName = "id"))
    private List<Revision> revision = new ArrayList<>();

    public String getId()
    {
        return this.id;
    }

    public Map<String, Binary> getBinaries()
    {
        return Map.copyOf(this.binary);
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

    public void setBinaries(Map<String, Binary> binary)
    {
        this.binary = Map.copyOf(binary);
    }

    @Embeddable
    public static class Binary implements Serializable
    {
        @Serial
        private static final long serialVersionUID = -6119251919351052273L;

        @Column(columnDefinition = "blob", nullable = false)
        public byte[] binary = new byte[0];

        public static Binary from(byte[] binary)
        {
            var result = new Binary();
            result.binary = binary;
            return result;
        }
    }
}
