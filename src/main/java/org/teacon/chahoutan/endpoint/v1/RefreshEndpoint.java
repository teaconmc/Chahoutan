package org.teacon.chahoutan.endpoint.v1;

import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Image;
import org.teacon.chahoutan.entity.Post;
import org.teacon.chahoutan.repo.ImageRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Path("/v1/refresh")
@Produces(MediaType.APPLICATION_JSON)
public class RefreshEndpoint
{
    private static final AtomicReference<OffsetDateTime> startTimeRef = new AtomicReference<>();

    private final EntityManager manager;

    private final ImageRepository imageRepo;

    public RefreshEndpoint(EntityManager manager, ImageRepository imageRepo)
    {
        this.manager = manager;
        this.imageRepo = imageRepo;
    }

    @GET
    public Map<String, Object> get()
    {
        var startTime = startTimeRef.get();
        if (startTime != null)
        {
            return Map.of("refreshing", true, "start_time", startTime);
        }
        return Map.of("refreshing", false);
    }

    @POST
    @Transactional
    @RequireAuth
    public Map<String, Object> refresh()
    {
        var zoneOffset = ChahoutanConfig.POST_ZONE_OFFSET;
        if (startTimeRef.compareAndSet(null, Instant.now().truncatedTo(ChronoUnit.SECONDS).atOffset(zoneOffset)))
        {
            var session = Search.session(this.manager);
            var imageFuture = CompletableFuture.runAsync(() ->
            {
                for (var image : this.imageRepo.findAll())
                {
                    var binary = image.getBinaries().getOrDefault("bin", new byte[0]);
                    this.imageRepo.save(Image.from(binary, image.getId()));
                }
            });
            var sessionFuture = session.massIndexer(Post.class).start().toCompletableFuture();
            CompletableFuture.allOf(imageFuture, sessionFuture).thenRun(() -> startTimeRef.set(null));
        }
        return this.get();
    }
}
