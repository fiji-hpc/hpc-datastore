package cz.it4i.fiji.datastore.security;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/oauth-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OAuthGroupEndpoint {

    @Inject
    OAuthGroupService oAuthGroupService;
    private static final String GROUP_ID = "group_id";
    private static final String DATASET_ID = "dataset_id";
    private static final String CLIENT_ID = "client_id";

    @POST
    public Response createOAuthGroup(OAuthGroupDTO groupDTO) {
        oAuthGroupService.createOAuthGroup(groupDTO);
        return Response.ok().build();
    }

    @GET
    @Path("/{"+ GROUP_ID +"}")
    public Response getOAuthGroupById(@PathParam(GROUP_ID) long id) {
        OAuthGroup group = oAuthGroupService.getOAuthGroupById(id);
        if (group != null) {
            return Response.ok(group).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    public Response getAllOAuthGroups() {
        List<OAuthGroup> groups = oAuthGroupService.getAllOAuthGroups();
        return Response.ok(groups).build();
    }

    @PUT
    @Path("/{"+ GROUP_ID +"}")
    public Response updateOAuthGroup(@PathParam(GROUP_ID) long id, OAuthGroup updatedGroup) {
        OAuthGroup group = oAuthGroupService.getOAuthGroupById(id);
        if (group != null) {
            updatedGroup.setId((int)id);
            oAuthGroupService.updateOAuthGroup(updatedGroup);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{"+ GROUP_ID +"}")
    public Response deleteOAuthGroup(@PathParam(GROUP_ID) long id) {
        OAuthGroup group = oAuthGroupService.getOAuthGroupById(id);
        if (group != null) {
            oAuthGroupService.deleteOAuthGroup(group);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/{"+GROUP_ID+"}/users/{"+CLIENT_ID+"}")
    public Response addUserToGroup(@PathParam(GROUP_ID) int groupId, @PathParam(CLIENT_ID) long userId) {
        oAuthGroupService.addUserToGroup(groupId, userId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{"+GROUP_ID+"}/users/{"+CLIENT_ID+"}")
    public Response removeUserFromGroup(@PathParam(GROUP_ID) int groupId, @PathParam(CLIENT_ID) long userId) {
        if(oAuthGroupService.removeUserFromGroup(groupId, userId)) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("/{"+GROUP_ID+"}/datasets/{"+DATASET_ID+"}")
    public Response addDatasetToGroup(@PathParam(GROUP_ID) int groupId, @PathParam(DATASET_ID) String datasetId) {
        oAuthGroupService.addDatasetToGroup(groupId, datasetId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{"+GROUP_ID+"}/datasets/{"+DATASET_ID+"}")
    public Response removeDatasetFromGroup(@PathParam(GROUP_ID) int groupId, @PathParam(DATASET_ID) String datasetId) {
        oAuthGroupService.removeDatasetFromGroup(groupId, datasetId);
        return Response.ok().build();
    }

    @PUT
    @Path("/{"+GROUP_ID+"}/setPermission/{"+DATASET_ID+"}")
    public Response changeGroupPermission(@PathParam(GROUP_ID) long groupId, @PathParam(DATASET_ID) String permissionType) {
        if(oAuthGroupService.changeGroupPermission(groupId,permissionType))
        {
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

}
