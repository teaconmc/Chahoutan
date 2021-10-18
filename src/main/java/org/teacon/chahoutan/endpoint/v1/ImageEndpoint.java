package org.teacon.chahoutan.endpoint.v1;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Image;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Path("/v1/images")
@Produces(MediaType.APPLICATION_JSON)
public class ImageEndpoint
{
    private final ImageRepository imageRepo;

    public ImageEndpoint(ImageRepository imageRepo)
    {
        this.imageRepo = imageRepo;
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{64}}")
    public Image.Response get(@PathParam("id") String id)
    {
        return this.imageRepo.findById(id).map(Image.Response::from).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{64}}.png")
    public Response getPng(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        return Response.ok(image.binary.getOrDefault("png", new Image.Binary()).binary, "image/png").build();
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{64}}.webp")
    public Response getWebp(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        return Response.ok(image.binary.getOrDefault("webp", new Image.Binary()).binary, "image/webp").build();
    }

    @GET
    @Transactional
    @RequireAuth
    @Path("/{id:[0-9a-fA-F]{64}}/revisions")
    public Iterator<Post.Response> iteratorOfRevisions(@PathParam("id") String id)
    {
        var image = this.imageRepo.findById(id).orElseThrow(NotFoundException::new);
        return image.revision.stream().collect(Collectors.toMap(r -> r.id,
                Post.Response::from, (p, q) -> p, LinkedHashMap::new)).values().iterator();
    }

    @POST
    @Transactional
    @RequireAuth
    public Image.Response add(@RequestBody byte[] input)
    {
        var id = Image.toHash(input);
        if (!this.imageRepo.existsById(id))
        {
            var image = this.imageRepo.save(Image.from(input, id));
            return Image.Response.from(image);
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
        if (image.revision.isEmpty())
        {
            this.imageRepo.delete(image);
            return Map.of();
        }
        throw new BadRequestException("The revision of image is not empty");
    }
}
