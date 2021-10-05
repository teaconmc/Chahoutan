package org.teacon.chahoutan.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.imageio.ImageIO;
import javax.persistence.*;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "chahoutan_images")
public class Image
{
    private static final char[] HEX_CODES = "0123456789abcdef".toCharArray();

    @Id
    @Column(name = "id", nullable = false, length = 64)
    public String id;

    @Column(name = "upload_time", nullable = false)
    public Instant uploadTime = Instant.EPOCH;

    @ElementCollection
    @MapKeyColumn(name = "suffix", length = 16)
    @CollectionTable(name = "chahoutan_image_binaries", joinColumns = @JoinColumn(name = "image_id", referencedColumnName = "id"))
    public Map<String, Binary> binary = new HashMap<>();

    @ManyToMany
    @OrderBy("creationTime DESC")
    @CollectionTable(name = "chahoutan_post_images", joinColumns = @JoinColumn(name = "image_id", referencedColumnName = "id"))
    public List<Revision> revision = new ArrayList<>();

    public static Optional<Image> from(Request request, ImageRepository imageRepo)
    {
        var id = request.id.toLowerCase(Locale.ROOT);
        if (!id.isEmpty())
        {
            return imageRepo.findById(id);
        }
        var png = request.png.toLowerCase(Locale.ROOT);
        if (!png.endsWith(".png"))
        {
            return imageRepo.findById(png.substring(0, png.length() - 4));
        }
        var webp = request.png.toLowerCase(Locale.ROOT);
        if (!webp.endsWith(".webp"))
        {
            return imageRepo.findById(webp.substring(0, webp.length() - 5));
        }
        return Optional.empty();
    }

    public static Image from(byte[] input)
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

                var stringBuilder = new StringBuilder();
                var hash = MessageDigest.getInstance("SHA-256").digest(input);
                for (byte b : hash)
                {
                    stringBuilder.append(HEX_CODES[b >> 4 & 0xF]);
                    stringBuilder.append(HEX_CODES[b & 0xF]);
                }

                var pngOutput = new ByteArrayOutputStream();
                var webpOutput = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", pngOutput);
                ImageIO.write(bufferedImage, "webp", webpOutput);

                var image = new Image();
                image.uploadTime = Instant.now();
                image.id = stringBuilder.toString();
                image.binary = Map.of(
                        "bin", Binary.from(input.clone()),
                        "png", Binary.from(pngOutput.toByteArray()),
                        "webp", Binary.from(webpOutput.toByteArray()));

                return image;
            }
            throw new IOException("Unsupported image type");
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            throw new BadRequestException(e);
        }
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

    public static record Request(@JsonProperty(value = "id") @DefaultValue("") String id,
                                 @JsonProperty(value = "png") @DefaultValue("") String png,
                                 @JsonProperty(value = "webp") @DefaultValue("") String webp)
    {
    }

    public static record Response(@JsonProperty(value = "id") String id,
                                  @JsonProperty(value = "png") String png,
                                  @JsonProperty(value = "webp") String webp)
    {
        public static Response from(Image image)
        {
            return new Response(image.id, image.id + ".png", image.id + ".webp");
        }
    }
}
