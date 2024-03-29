package org.teacon.chahoutan.entity;

import org.teacon.chahoutan.ChahoutanConfig;

import javax.imageio.ImageIO;
import javax.persistence.*;
import javax.ws.rs.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_images")
@SecondaryTable(name = "chahoutan_image_sizes", pkJoinColumns = @PrimaryKeyJoinColumn(name = "image_id"))
public class Image
{
    @Id
    @Column(name = "id", columnDefinition = "char(64)", nullable = false)
    private String id;

    @Column(name = "width", table = "chahoutan_image_sizes", columnDefinition = "smallint", nullable = false)
    private Integer width = 0;

    @Column(name = "height", table = "chahoutan_image_sizes", columnDefinition = "smallint", nullable = false)
    private Integer height = 0;

    @Column(name = "upload_time", columnDefinition = "timestamptz", nullable = false)
    private ZonedDateTime uploadTime = Instant.EPOCH.atZone(ChahoutanConfig.POST_ZONE_ID);

    @ElementCollection
    @MapKeyColumn(name = "suffix", length = 16)
    @Column(name = "image_binary", columnDefinition = "bytea", nullable = false)
    @CollectionTable(name = "chahoutan_image_binaries", joinColumns = @JoinColumn(name = "image_id"))
    private Map<String, byte[]> binaries = new HashMap<>();

    @ManyToMany
    @OrderBy("creationTime DESC")
    @JoinTable(name = "chahoutan_post_images",
            joinColumns = @JoinColumn(name = "image_id", updatable = false, insertable = false),
            inverseJoinColumns = @JoinColumn(name = "revision_id", updatable = false, insertable = false))
    private List<Revision> revisions = new ArrayList<>();

    public static Image from(byte[] input, String imageId)
    {
        try
        {
            var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input));
            var readers = ImageIO.getImageReaders(stream);
            if (readers.hasNext())
            {
                var reader = readers.next();
                reader.setInput(stream, true, true);

                var param = reader.getDefaultReadParam();
                var bufferedImage = reader.read(0, param);

                var png = new ByteArrayOutputStream();
                var webp = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", png);
                ImageIO.write(bufferedImage, "webp", webp);

                var image = new Image();
                image.setId(imageId);
                image.setUploadTime(Instant.now());
                image.setWidth(bufferedImage.getWidth());
                image.setHeight(bufferedImage.getHeight());
                image.setBinaries(Map.of("bin", input.clone(), "png", png.toByteArray(), "webp", webp.toByteArray()));

                return image;
            }
            throw new IOException("Unsupported image type");
        }
        catch (IOException e)
        {
            throw new BadRequestException(e);
        }
    }

    public String getId()
    {
        return this.id;
    }

    public Integer getWidth()
    {
        return this.width == null ? 0 : this.width;
    }

    public Integer getHeight()
    {
        return this.height == null ? 0 : this.height;
    }

    public Map<String, byte[]> getBinaries()
    {
        return Map.copyOf(this.binaries);
    }

    public List<Revision> getRevisions()
    {
        return List.copyOf(this.revisions);
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setWidth(Integer width)
    {
        this.width = width;
    }

    public void setHeight(Integer height)
    {
        this.height = height;
    }

    public void setUploadTime(Instant uploadTime)
    {
        this.uploadTime = uploadTime.atZone(ChahoutanConfig.POST_ZONE_ID);
    }

    public void setBinaries(Map<String, byte[]> binary)
    {
        this.binaries = new HashMap<>(binary);
    }
}
