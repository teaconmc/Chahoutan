package org.teacon.chahoutan.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class ErrorExceptionMapper implements ExceptionMapper<Exception>
{
    private static final Logger LOGGER = LogManager.getLogger(ErrorExceptionMapper.class);

    @Override
    public Response toResponse(Exception e)
    {
        LOGGER.info(e);
        var status = Response.Status.INTERNAL_SERVER_ERROR;
        if (e instanceof WebApplicationException e2)
        {
            status = e2.getResponse().getStatusInfo().toEnum();
        }
        if (e instanceof JsonProcessingException)
        {
            status = Response.Status.BAD_REQUEST;
        }
        var body = Map.of("error", status.getReasonPhrase());
        return Response.status(status).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
