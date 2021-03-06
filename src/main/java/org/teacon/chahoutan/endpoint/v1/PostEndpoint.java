package org.teacon.chahoutan.endpoint.v1;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.entity.Revision;
import org.teacon.chahoutan.network.ImageRequest;
import org.teacon.chahoutan.network.ImageResponse;
import org.teacon.chahoutan.network.PostRequest;
import org.teacon.chahoutan.network.PostResponse;
import org.teacon.chahoutan.repo.ImageRepository;
import org.teacon.chahoutan.repo.PostRepository;
import org.teacon.chahoutan.repo.RevisionRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public Iterator<PostResponse> iterator(@QueryParam("q") String query, @QueryParam("until") Integer until)
    {
        var lastId = Post.getLastPublicPostId(until);
        if (query == null || query.isEmpty())
        {
            return this.postRepo.findFirst20PostsByIdLessThanEqualAndRevisionNotNullOrderByIdDesc(lastId).stream()
                    .map(PostResponse::from).iterator();
        }
        else
        {
            var session = Search.session(this.manager);
            return session.search(Post.class)
                    .where(f -> f.bool()
                            .minimumShouldMatchNumber(1)
                            .must(f2 -> f2.range().field("id").atMost(lastId))
                            .should(f2 -> f2.match().field("text").matching(query, ValueConvert.NO))
                            .should(f2 -> f2.match().field("editor").matching(query, ValueConvert.NO).boost(10.0f))
                            .should(f2 -> f2.match().field("title").matching(query, ValueConvert.NO).boost(100.0f)))
                    .fetch(20).hits().stream()
                    .map(PostResponse::from).iterator();
        }
    }

    @GET
    @Path("/{id:[1-9][0-9]*}")
    public PostResponse get(@PathParam("id") Integer id)
    {
        return this.postRepo.findByIdAndRevisionNotNull(id)
                .filter(p -> p.getId() <= Post.getLastPublicPostId(null))
                .map(PostResponse::from).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}")
    public PostResponse get(@PathParam("id") UUID id)
    {
        return this.revisionRepo.findById(id)
                .map(PostResponse::from).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[1-9][0-9]*}/editors")
    public List<String> getEditors(@PathParam("id") Integer id)
    {
        return this.postRepo.findByIdAndRevisionNotNull(id)
                .filter(p -> p.getId() <= Post.getLastPublicPostId(null))
                .map(Post::getEditors).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[1-9][0-9]*}/images")
    public List<ImageResponse> getImages(@PathParam("id") Integer id)
    {
        return this.postRepo.findByIdAndRevisionNotNull(id)
                .filter(p -> p.getId() <= Post.getLastPublicPostId(null))
                .map(p -> p.getRevision().getImages().stream().map(ImageResponse::from).toList()).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}/images")
    public List<ImageResponse> getImages(@PathParam("id") UUID id)
    {
        return this.revisionRepo.findById(id)
                .map(r -> r.getImages().stream().map(ImageResponse::from).toList()).orElseThrow(NotFoundException::new);
    }

    @POST
    @RequireAuth
    public PostResponse add(@RequestBody PostRequest body)
    {
        var post = body.toPost(this.imageRepo);
        return PostResponse.from(this.postRepo.save(post));
    }

    @DELETE
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}")
    public Map<String, Void> remove(@PathParam("id") Integer id)
    {
        var post = this.postRepo.findById(id).orElseThrow(NotFoundException::new);
        post.setRevision(null); // detach revisions
        this.postRepo.save(post);
        return Map.of();
    }

    @GET
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}/revisions")
    public Iterator<PostResponse> iteratorOfRevisions(@PathParam("id") Integer id)
    {
        return this.postRepo.findById(id).stream()
                .map(this.revisionRepo::findRevisionsByPostOrderByCreationTimeDesc)
                .flatMap(List::stream).map(PostResponse::from).iterator();
    }

    @GET
    @RequireAuth
    @Path("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}/revisions")
    public Iterator<PostResponse> iteratorOfRevisions(@PathParam("id") UUID id)
    {
        return this.revisionRepo.findById(id).stream().map(Revision::getPost)
                .map(this.revisionRepo::findRevisionsByPostOrderByCreationTimeDesc)
                .flatMap(List::stream).map(PostResponse::from).iterator();
    }

    @PUT
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}/editors")
    public List<String> setEditors(@PathParam("id") Integer id, @RequestBody List<String> body)
    {
        var post = this.postRepo.findByIdAndRevisionNotNull(id).orElseThrow(NotFoundException::new);
        post.setEditors(body);
        return this.postRepo.save(post).getEditors();
    }

    @PUT
    @RequireAuth
    @Path("/{id:[1-9][0-9]*}/images")
    public List<ImageResponse> setImages(@PathParam("id") Integer id, @RequestBody List<ImageRequest> body)
    {
        var post = this.postRepo.findByIdAndRevisionNotNull(id).orElseThrow(NotFoundException::new);
        post.getRevision().setImages(body.stream().map(request -> request.toImage(this.imageRepo)).toList());
        return this.postRepo.save(post).getRevision().getImages().stream().map(ImageResponse::from).toList();
    }
}
