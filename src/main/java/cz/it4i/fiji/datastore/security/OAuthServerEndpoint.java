package cz.it4i.fiji.datastore.security;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("/oauth-servers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OAuthServerEndpoint {

    @Inject
    OAuthServerService oauthServerService;
    private static final String SERVER_ID = "client_id";
    @GET
    public Response getAllOAuthServers() {
        List<OAuthServerNew> servers = oauthServerService.getAllOAuthServers();
        return Response.ok(servers).build();
    }

    @GET
    @Path("/{"+SERVER_ID+"}")
    public Response getServerById(@PathParam(SERVER_ID) Long id) {
        Optional<OAuthServerNew> serverOptional = oauthServerService.getOAuthServerById(id);
        OAuthServerNew server = serverOptional.orElse(null);
        if (server != null) {
            return Response.ok(server).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Path("/{"+SERVER_ID+"}")
    public Response updateOAuthServer(@PathParam(SERVER_ID) Long id, OAuthServerNew oauthServer) {
        boolean success = oauthServerService.updateOAuthServer(id, oauthServer);
        if (success) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{"+SERVER_ID+"}")
    public Response deleteOAuthServerById(@PathParam(SERVER_ID) Long id) {
        boolean success = oauthServerService.deleteOAuthServerById(id);
        if (success) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/create")
    public Response createOAuthServer(OAuthServerNew oauthServer) {
        oauthServerService.createOAuthServer(oauthServer);
        return Response.status(Response.Status.CREATED).build();
    }
}
