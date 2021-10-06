package org.teacon.chahoutan.endpoint.v1;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.teacon.chahoutan.auth.RequireAuth;
import org.teacon.chahoutan.entity.Metadata;
import org.teacon.chahoutan.repo.MetadataRepository;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;
import java.util.List;

@Component
@Path("/v1/metadata")
@Produces(MediaType.APPLICATION_JSON)
public class MetadataEndpoint
{
    private final MetadataRepository metadataRepo;

    public MetadataEndpoint(MetadataRepository metadataRepo)
    {
        this.metadataRepo = metadataRepo;
    }

    @GET
    public Iterator<Metadata> iterator()
    {
        return this.metadataRepo.findAllByOrderByIdAsc().iterator();
    }

    @POST
    @RequireAuth
    public Iterator<Metadata> add(@RequestBody Metadata body)
    {
        this.metadataRepo.save(body);
        return this.metadataRepo.findAllByOrderByIdAsc().iterator();
    }

    @PUT
    @RequireAuth
    @Transactional
    public Iterator<Metadata> set(@RequestBody List<Metadata> body)
    {
        this.metadataRepo.deleteAll();
        this.metadataRepo.saveAll(body);
        return this.metadataRepo.findAllByOrderByIdAsc().iterator();
    }
}
