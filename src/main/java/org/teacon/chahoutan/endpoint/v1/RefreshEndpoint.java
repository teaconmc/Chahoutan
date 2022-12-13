package org.teacon.chahoutan.endpoint.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.teacon.chahoutan.ChahoutanConfig;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.repo.SearchIndexRepository;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Path("/v1/refresh")
@Produces(MediaType.APPLICATION_JSON)
public class RefreshEndpoint
{
    private static final Logger LOGGER = LogManager.getLogger(RefreshEndpoint.class);

    private static final AtomicReference<ZonedDateTime> startTimeRef = new AtomicReference<>();

    private final PlatformTransactionManager transactionManager;
    private final SearchIndexRepository searchIndexRepo;

    public RefreshEndpoint(PlatformTransactionManager transactionManager,
                           SearchIndexRepository searchIndexRepo)
    {
        this.transactionManager = transactionManager;
        this.searchIndexRepo = searchIndexRepo;
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
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS).atZone(ChahoutanConfig.POST_ZONE_ID);
        if (startTimeRef.compareAndSet(null, now))
        {
            var template = new TransactionTemplate(this.transactionManager);
            var future = CompletableFuture.runAsync(() -> template.executeWithoutResult(status ->
            {
                LOGGER.info("Start refreshing ...");
                this.searchIndexRepo.createSearchIndex();
                for (var postId : this.searchIndexRepo.selectPostIds())
                {
                    this.searchIndexRepo.deleteAllByPostId(postId);
                    this.searchIndexRepo.refreshAllByPostId(ChahoutanConfig.PG_FTS_CONFIG, postId);
                }
                LOGGER.info("Complete refreshing ...");
            }));
            future.whenComplete((v, t) -> startTimeRef.set(null));
        }
        return this.get();
    }
}
