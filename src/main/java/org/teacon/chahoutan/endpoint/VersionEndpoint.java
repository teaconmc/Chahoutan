package org.teacon.chahoutan.endpoint;

import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Component
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class VersionEndpoint
{
    private static final int VERSION = 1;

    @GET
    public Map<String, Integer> version()
    {
        return Map.of("version", VERSION);
    }
}
