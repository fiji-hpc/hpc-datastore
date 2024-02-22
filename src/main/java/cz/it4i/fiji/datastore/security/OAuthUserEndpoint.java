package cz.it4i.fiji.datastore.security;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

@Path("/oauth-users")
@Produces(MediaType.APPLICATION_JSON)
public class OAuthUserEndpoint {

    @Inject
    OAuthUserService oauthUserService;

    private static final String CLIENT_ID = "client_id";

    @GET
    public List<User> getAllOAuthUsers() {
        List<User> list = oauthUserService.getAllOAuthUsers();
        for (User oa : list) {
            oa.setClientID(null);
        }
        return list;
    }

    @GET
    @Path("/{"+CLIENT_ID+"}")
    public Response getUserById(@PathParam(CLIENT_ID) Long id) {
        List<User> list = oauthUserService.getAllOAuthUsers();
        for (User user : list) {
            if (user.getId().equals(id)) {
                return Response.ok(user).build();
            }
        }
        return Response.ok(null).build();
    }

    @PUT
    @Path("/{"+CLIENT_ID+"}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOAuthUser(@PathParam(CLIENT_ID) String clientID, User oauthUser) {
        oauthUserService.updateOAuthServer(Long.valueOf(clientID), oauthUser);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{"+CLIENT_ID+"}")
    public Response deleteOAuthUserById(@PathParam(CLIENT_ID) String clientID) {
        oauthUserService.deleteOAuthUserById(Long.valueOf(clientID));
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOAuthUser(User oauthUser) {
        oauthUserService.createOAuthUser(oauthUser);
        return Response.status(Response.Status.CREATED).build();
    }
}
