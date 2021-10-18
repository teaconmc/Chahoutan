package org.teacon.chahoutan.provider;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.time.Duration;

@Provider
public class CorsResponseFilter implements ContainerResponseFilter
{
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext context)
    {
        context.getHeaders().add("Access-Control-Allow-Credentials", true);
        context.getHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");
        context.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        context.getHeaders().add("Access-Control-Allow-Origin", requestContext.getHeaderString("Origin"));
        context.getHeaders().add("Access-Control-Max-Age", Duration.ofDays(60).toSeconds());
    }
}
