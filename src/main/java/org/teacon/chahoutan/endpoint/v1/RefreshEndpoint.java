package org.teacon.chahoutan.endpoint.v1;

import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Post;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Path("/v1/refresh")
@Produces(MediaType.APPLICATION_JSON)
public class RefreshEndpoint
{
    private static final AtomicBoolean isRefreshing = new AtomicBoolean();

    private final EntityManager manager;

    public RefreshEndpoint(EntityManager manager)
    {
        this.manager = manager;
    }

    @GET
    public Map<String, Boolean> get()
    {
        return Map.of("refreshing", isRefreshing.get());
    }

    @POST
    @Transactional
    @RequireAuth
    public Map<String, Boolean> refresh()
    {
        if (isRefreshing.compareAndSet(false, true))
        {
            var session = Search.session(this.manager);
            session.massIndexer(Post.class).start().thenRun(() -> isRefreshing.set(false));
        }
        return Map.of("refreshing", isRefreshing.get());
    }
}
