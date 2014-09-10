package com.brightcove.analytics.haproxy.api;

import com.brightcove.analytics.haproxy.api.model.LoadbalancedApplication;
import com.brightcove.analytics.haproxy.ZookeeperStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static javax.ws.rs.core.Response.Status.*;

@Path("/apps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppsResource {

    private static final Logger log = LoggerFactory.getLogger(AppsResource.class);
    private final ZookeeperStore store;

    public AppsResource(ZookeeperStore store) {
        this.store = store;
    }

    @POST
    public Response create(@Valid LoadbalancedApplication application) {
        try {
            String id = store.put(application);
            return Response.created(UriBuilder.fromResource(AppsResource.class).build(id)).build();
        } catch (Exception e) {
            log.warn("Unable to save application %s", application, e);
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }

    @Path("/{id}")
    @PUT
    public Response put(@PathParam("id") String appId, @Valid LoadbalancedApplication application) {
        try {
            LoadbalancedApplication app = store.get(appId);
            if (app == null) {
                throw new WebApplicationException(NOT_FOUND);
            }
            app.setId(application.getId());
            app.setTemplate(application.getTemplate());
            app.setSslCertificate(application.getSslCertificate());
            store.put(app);
            return Response.status(ACCEPTED).build();
        } catch (Exception e) {
            log.warn("Unable to get data from zookeeper", e);
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/{id}")
    public LoadbalancedApplication get(@PathParam("id") String appId) {
        try {
            LoadbalancedApplication app = store.get(appId);
            if (app == null) {
                throw new WebApplicationException(NOT_FOUND);
            }
            return app;
        } catch (Exception e) {
            log.warn("Unable to get data from zookeeper", e);
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String appId) {
        try {
            LoadbalancedApplication app = store.get(appId);
            if (app == null) {
                throw new WebApplicationException(NOT_FOUND);
            }
            store.delete(appId);
            return Response.status(NO_CONTENT).build();
        } catch (Exception e) {
            log.warn("Unable to get data from zookeeper", e);
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }

}
