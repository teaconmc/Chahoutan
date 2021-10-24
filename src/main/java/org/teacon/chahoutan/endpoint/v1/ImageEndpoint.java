package org.teacon.chahoutan.endpoint.v1;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Image;
import org.teacon.chahoutan.entity.Revision;
import org.teacon.chahoutan.network.ImageResponse;
import org.teacon.chahoutan.network.PostResponse;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("/v1/images")
@Produces(MediaType.APPLICATION_JSON)
public class ImageEndpoint
{
    private static final char[] HEX_CODES = "0123456789abcdef".toCharArray();

    private final ImageRepository imageRepo;

    public ImageEndpoint(ImageRepository imageRepo)
    {
        this.imageRepo = imageRepo;
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{64}}")
    public ImageResponse get(@PathParam("id") String id)
    {
        return this.imageRepo.findById(id).map(ImageResponse::from).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{64}}.png")
    public Response getPng(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        return Response.ok(image.getBinaries().getOrDefault("png", new Image.Binary()).binary, "image/png").build();
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{64}}.webp")
    public Response getWebp(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        return Response.ok(image.getBinaries().getOrDefault("webp", new Image.Binary()).binary, "image/webp").build();
    }

    @GET
    @Transactional
    @RequireAuth
    @Path("/{id:[0-9a-fA-F]{64}}/revisions")
    public Iterator<PostResponse> iteratorOfRevisions(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        return image.getRevisions().stream().collect(Collectors.toMap(Revision::getId,
                PostResponse::from, (p, q) -> p, LinkedHashMap::new)).values().iterator();
    }

    @POST
    @Transactional
    @RequireAuth
    public ImageResponse add(@RequestBody byte[] input)
    {
        var id = getHash(input);
        if (!this.imageRepo.existsById(id))
        {
            var image = this.imageRepo.save(getImage(input, id));
            return ImageResponse.from(image);
        }
        throw new ClientErrorException(Response.Status.CONFLICT);
    }

    @DELETE
    @Transactional
    @RequireAuth
    @Path("/{id:[0-9a-fA-F]{64}}")
    public Map<String, Void> remove(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        if (image.getRevisions().isEmpty())
        {
            this.imageRepo.delete(image);
            return Map.of();
        }
        throw new BadRequestException("The revision of image is not empty");
    }

    private String getHash(byte[] input)
    {
        try
        {
            var hash = MessageDigest.getInstance("SHA-256").digest(input);
            var stringBuilder = new StringBuilder();
            for (byte b : hash)
            {
                stringBuilder.append(HEX_CODES[b >> 4 & 0xF]);
                stringBuilder.append(HEX_CODES[b & 0xF]);
            }
            return stringBuilder.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new InternalServerErrorException(e);
        }
    }

    private Image getImage(byte[] input, String imageId)
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
                image.setUploadTime(Instant.now());
                image.setBinaries(Map.of(
                        "bin", Image.Binary.from(input.clone()),
                        "png", Image.Binary.from(pngOutput.toByteArray()),
                        "webp", Image.Binary.from(webpOutput.toByteArray())));

                return image;
            }
            throw new IOException("Unsupported image type");
        }
        catch (IOException e)
        {
            throw new BadRequestException(e);
        }
    }
}
