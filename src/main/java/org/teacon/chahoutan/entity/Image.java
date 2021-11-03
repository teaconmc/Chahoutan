package org.teacon.chahoutan.entity;

import javax.imageio.ImageIO;
import javax.persistence.*;
import javax.ws.rs.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Access(AccessType.FIELD)
@Table(name = "chahoutan_images")
@SecondaryTable(name = "chahoutan_image_sizes", pkJoinColumns = @PrimaryKeyJoinColumn(name = "image_id", referencedColumnName = "id"))
public class Image
{
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "width", table = "chahoutan_image_sizes", nullable = false)
    private Integer width = 0;

    @Column(name = "height", table = "chahoutan_image_sizes", nullable = false)
    private Integer height = 0;

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

    public static Image from(byte[] input, String imageId, List<Revision> revisions)
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

                var pngOutput = new ByteArrayOutputStream();
                var webpOutput = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", pngOutput);
                ImageIO.write(bufferedImage, "webp", webpOutput);

                var image = new Image();
                image.setId(imageId);
                image.setWidth(bufferedImage.getWidth());
                image.setHeight(bufferedImage.getHeight());

                var uploadTime = Instant.now();
                image.setUploadTime(uploadTime);

                var pngBytes = pngOutput.toByteArray();
                var webpBytes = webpOutput.toByteArray();
                image.setBinaries(Map.of("bin", input.clone(), "png", pngBytes, "webp", webpBytes));

                image.setRevisions(revisions);

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
        return List.copyOf(this.revision);
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
        this.uploadTime = uploadTime;
    }

    public void setBinaries(Map<String, byte[]> binary)
    {
        this.binaries = Map.copyOf(binary);
    }

    public void setRevisions(List<Revision> revisions)
    {
        this.revision = List.copyOf(revisions);
    }
}
