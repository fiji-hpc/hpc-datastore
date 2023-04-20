package cz.it4i.fiji.datastore.security;
import com.google.gson.Gson;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/oauth-users")
public class OAuthUserEndpoint {

    @Inject
    OAuthUserService oauthUserService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<OAuthUserNew> getAllOAuthUsers() {
        return oauthUserService.getAllOAuthUsers();
    }
    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOAuthUsersJSON() {
        List<OAuthUserNew> list=oauthUserService.getAllOAuthUsers();
        for(OAuthUserNew oa :list)
        {
            //TODO Anonimize tokens
            oa.setClientID("censored");
            oa.setClientSecret("censored");
        }
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }


    @GET
    @Path("/{client_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthUserNew getUserByAlias(@PathParam("client_id") String alias)
    {
        List<OAuthUserNew> list=oauthUserService.getAllOAuthUsers();
        for(int i=0;i<list.size()-1;i++) {
            if(list.get(i).getClientID().equals(alias))
            {
                return list.get(i);
            }
        }
        return new OAuthUserNew();
    }
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateOAuthUser(@PathParam("id") String ClientID, OAuthUserNew oauthUser) {
        oauthUserService.updateOAuthServer(Long.valueOf(ClientID), oauthUser);
    }

    @DELETE
    @Path("/{id}")
    public void deleteOAuthUserById(@PathParam("id") String ClientID) {
        oauthUserService.deleteOAuthUserById(Long.valueOf(ClientID));
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOAuthUser(OAuthUserNew oauthUser) {

        oauthUserService.createOAuthUser(oauthUser);
    }


}