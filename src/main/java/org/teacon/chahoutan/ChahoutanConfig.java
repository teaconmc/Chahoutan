package org.teacon.chahoutan;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;
import org.teacon.chahoutan.auth.RequireAuthFilter;
import org.teacon.chahoutan.endpoint.FeedEndpoint;
import org.teacon.chahoutan.endpoint.VersionEndpoint;
import org.teacon.chahoutan.endpoint.v1.ImageEndpoint;
import org.teacon.chahoutan.endpoint.v1.MetadataEndpoint;
import org.teacon.chahoutan.endpoint.v1.PostEndpoint;
import org.teacon.chahoutan.endpoint.v1.RefreshEndpoint;
import org.teacon.chahoutan.provider.ErrorExceptionMapper;
import org.teacon.chahoutan.provider.JacksonContextResolver;

@Component
public class ChahoutanConfig extends ResourceConfig
{
    public ChahoutanConfig()
    {
        // endpoints
        this.register(FeedEndpoint.class);
        this.register(VersionEndpoint.class);
        this.register(ImageEndpoint.class);
        this.register(MetadataEndpoint.class);
        this.register(PostEndpoint.class);
        this.register(RefreshEndpoint.class);
        // providers
        this.register(new RequireAuthFilter());
        this.register(new ErrorExceptionMapper());
        this.register(new JacksonContextResolver());
        this.register(new JacksonJaxbJsonProvider());
    }
}
