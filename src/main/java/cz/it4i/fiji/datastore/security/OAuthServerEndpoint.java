package cz.it4i.fiji.datastore.security;
import com.google.gson.Gson;

import javax.inject.Inject;
        import javax.ws.rs.*;
        import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/oauth-servers")
public class OAuthServerEndpoint {

    @Path("/hello")
    @GET
    public Response hello() {
        return Response.ok("<h1>Hello world</h1>").build();
    }

    @Inject
    OAuthServerService oauthServerService;

    @GET

    //@Produces(MediaType.APPLICATION_JSON)
    public Response getAllOAuthServers() {
        String vysledek="";
        List<OAuthServerNew> list=oauthServerService.getAllOAuthServers();
        for(int i=0;i<list.size()-1;i++){
            vysledek=vysledek+"<a href="+list.get(i).getAlias()+">"+list.get(i).getAlias()+"</a>";
        }
        return Response.ok("<p>"+vysledek+"</p>").build();
    }
    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOAuthServersJSON() {
        List<OAuthServerNew> list = oauthServerService.getAllOAuthServers();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/{alias}")
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthServerNew getServerByAlias(@PathParam("alias") String alias)
    {
        List<OAuthServerNew> list=oauthServerService.getAllOAuthServers();
        for(int i=0;i<list.size()-1;i++) {
            if(list.get(i).getAlias().equals(alias))
            {
                return list.get(i);
            }
        }
        return new OAuthServerNew();
    }
    /*
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthServerNew getOAuthServerById(@PathParam("id") Long id) {
        return oauthServerService.getOAuthServerById(id);
    }
    */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateOAuthServer(@PathParam("id") Long id, OAuthServerNew oauthServer) {
        oauthServerService.updateOAuthServer(id, oauthServer);

    }

    @DELETE
    @Path("/{id}")
    public void deleteOAuthServerById(@PathParam("id") Long id) {
        oauthServerService.deleteOAuthServerById(id);
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createOAuthServer(OAuthServerNew oauthServer) {

        oauthServerService.createOAuthServer(oauthServer);
    }


}