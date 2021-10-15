package org.teacon.chahoutan.endpoint;

import org.springframework.stereotype.Component;
import org.teacon.chahoutan.ChahoutanConfig;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Component
@Path("/")
public class VersionEndpoint
{
    @GET
    @Path("/favicon.ico")
    @Produces("image/x-icon")
    public byte[] favicon()
    {
        try
        {
            var compressed = ChahoutanConfig.ICO_BINARY_WITH_GZIP_COMPRESSED;
            return new GZIPInputStream(new ByteArrayInputStream(compressed)).readAllBytes();
        }
        catch (IOException e)
        {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> version()
    {
        return Map.of("version", ChahoutanConfig.VERSION);
    }
}
