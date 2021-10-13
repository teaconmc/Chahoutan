package org.teacon.chahoutan.endpoint.v1;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Image;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.repo.ImageRepository;
import org.teacon.chahoutan.repo.PostRepository;
import org.teacon.chahoutan.repo.RevisionRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

@Component
@Path("/v1/posts")
@Produces(MediaType.APPLICATION_JSON)
public class PostEndpoint
{
    private final EntityManager manager;
    private final ImageRepository imageRepo;
    private final PostRepository postRepo;
    private final RevisionRepository revisionRepo;

    public PostEndpoint(EntityManager manager,
                        ImageRepository imageRepo,
                        PostRepository postRepo,
                        RevisionRepository revisionRepo)
    {
        this.manager = manager;
        this.imageRepo = imageRepo;
        this.postRepo = postRepo;
        this.revisionRepo = revisionRepo;
    }

    @GET
    @Transactional
    public Iterator<Post.Response> iterator(@QueryParam("q") String query, @QueryParam("until") Integer until)
    {
        var lastId = Post.getLastPublicPostId(until);
        if (query == null || query.isEmpty())
        {
            return this.postRepo.findFirst20PostsByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(lastId).stream()
                    .map(Post.Response::from).iterator();
        }
        else
        {
            var session = Search.session(this.manager);
            return session.search(Post.class)
                    .where(f -> f.bool()
                            .must(f2 -> f2.range().field("id").atMost(lastId))
                            .must(f2 -> f2.match().field("text").matching(query, ValueConvert.NO)))
                    .fetch(20).hits().stream()
                    .map(Post.Response::from).iterator();
        }
    }

    @GET
    @Path("/{id:[1-9][0-9]*}")
    public Post.Response get(@PathParam("id") Integer id)
    {
        return this.postRepo.findByIdAndRevisionNotNull(id)
                .filter(p -> p.id <= Post.getLastPublicPostId(null))
                .map(Post.Response::from).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}")
    public Post.Response get(@PathParam("id") UUID id)
    {
        return this.revisionRepo.findById(id)
                .map(Post.Response::from).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[1-9][0-9]*}/editors")
    public List<String> getEditors(@PathParam("id") Integer id)
    {
        return this.postRepo.findByIdAndRevisionNotNull(id)
                .filter(p -> p.id <= Post.getLastPublicPostId(null))
                .map(Post::getSortedEditors).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[1-9][0-9]*}/images")
    public List<Image.Response> getImages(@PathParam("id") Integer id)
    {
        return this.postRepo.findByIdAndRevisionNotNull(id)
                .filter(p -> p.id <= Post.getLastPublicPostId(null))
                .map(p -> p.revision.image.stream().map(Image.Response::from).toList()).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}/images")
    public List<Image.Response> getImages(@PathParam("id") UUID id)
    {
        return this.revisionRepo.findById(id)
                .map(r -> r.image.stream().map(Image.Response::from).toList()).orElseThrow(NotFoundException::new);
    }

    @POST
    @RequireAuth
    public Post.Response add(@RequestBody Post.Request body)
    {
        var post = Post.from(body, this.imageRepo);
        return Post.Response.from(this.postRepo.save(post));
    }

    @DELETE
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}")
    public Map<String, Void> remove(@PathParam("id") Integer id)
    {
        var post = this.postRepo.findById(id).orElseThrow(NotFoundException::new);
        post.revision = null; // detach revisions
        this.postRepo.save(post);
        return Map.of();
    }

    @GET
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}/revisions")
    public Iterator<Post.Response> iteratorOfRevisions(@PathParam("id") Integer id)
    {
        return this.postRepo.findById(id).stream()
                .map(this.revisionRepo::findRevisionsByPostOrderByCreationTimeDesc)
                .flatMap(List::stream).map(Post.Response::from).iterator();
    }

    @GET
    @RequireAuth
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}/revisions")
    public Iterator<Post.Response> iteratorOfRevisions(@PathParam("id") UUID id)
    {
        return this.revisionRepo.findById(id).stream().map(r -> r.post)
                .map(this.revisionRepo::findRevisionsByPostOrderByCreationTimeDesc)
                .flatMap(List::stream).map(Post.Response::from).iterator();
    }

    @PUT
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}/editors")
    public List<String> setEditors(@PathParam("id") Integer id, @RequestBody List<String> body)
    {
        var post = this.postRepo.findByIdAndRevisionNotNull(id).orElseThrow(NotFoundException::new);
        Post.setSortedEditors(post, body);
        return Post.getSortedEditors(this.postRepo.save(post));
    }

    @PUT
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}/images")
    public List<Image.Response> setImages(@PathParam("id") Integer id,  @RequestBody List<Image.Request> body)
    {
        var post = this.postRepo.findByIdAndRevisionNotNull(id).orElseThrow(NotFoundException::new);
        post.revision.image = body.stream()
                .map(r -> Image.from(r, imageRepo).orElseThrow(BadRequestException::new)).toList();
        return this.postRepo.save(post).revision.image.stream().map(Image.Response::from).toList();
    }
}
