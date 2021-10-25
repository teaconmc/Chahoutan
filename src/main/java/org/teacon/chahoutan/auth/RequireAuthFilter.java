package org.teacon.chahoutan.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
@RequireAuth
@Priority(Priorities.AUTHENTICATION)
public class RequireAuthFilter implements ContainerRequestFilter
{
    private static final Logger LOGGER = LogManager.getLogger(RequireAuthFilter.class);

    private static final char[] HEX_CODES = "0123456789abcdef".toCharArray();

    private static final Set<String> TOKENS;

    static
    {
        try
        {
            var path = Path.of("chahoutan-tokens.txt");
            if (!Files.exists(path))
            {
                var stringBuilder = new StringBuilder("# List of tokens arranged in rows\n");
                var random = new SecureRandom();
                for (var i = 0; i < 3; i++)
                {
                    for (var j = 0; j < 64; ++j)
                    {
                        stringBuilder.append(HEX_CODES[random.nextInt(16)]);
                    }
                    stringBuilder.append('\n');
                }
                Files.writeString(path, stringBuilder.toString(), StandardCharsets.UTF_8);
            }
            var tokens = Files.readAllLines(path, StandardCharsets.UTF_8);
            TOKENS = tokens.stream().map(String::strip)
                    .filter(s -> s.length() > 0 && !s.startsWith("#")).collect(Collectors.toUnmodifiableSet());
            LOGGER.info("Imported {} token(s) for authorization.", TOKENS.size());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void filter(ContainerRequestContext context)
    {
        var auth = context.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.toLowerCase(Locale.ROOT).startsWith("bearer ") || !TOKENS.contains(auth.substring(7)))
        {
            var body = Map.of("error", Response.Status.UNAUTHORIZED.getReasonPhrase());
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(body).build());
        }
    }
}
