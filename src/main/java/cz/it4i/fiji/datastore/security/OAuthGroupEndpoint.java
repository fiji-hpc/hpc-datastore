package cz.it4i.fiji.datastore.security;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/oauth-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OAuthGroupEndpoint {

    @Inject
    OAuthGroupService oAuthGroupService;

    @POST
    public Response createOAuthGroup(OAuthGroup group) {
        oAuthGroupService.createOAuthGroup(group);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/{id}")
    public Response getOAuthGroupById(@PathParam("id") int id) {
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
    @Path("/{id}")
    public Response updateOAuthGroup(@PathParam("id") int id, OAuthGroup updatedGroup) {
        OAuthGroup group = oAuthGroupService.getOAuthGroupById(id);
        if (group != null) {
            updatedGroup.setId(id);
            oAuthGroupService.updateOAuthGroup(updatedGroup);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteOAuthGroup(@PathParam("id") int id) {
        OAuthGroup group = oAuthGroupService.getOAuthGroupById(id);
        if (group != null) {
            oAuthGroupService.deleteOAuthGroup(group);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/{groupId}/users/{userId}")
    public Response addUserToGroup(@PathParam("groupId") int groupId, @PathParam("userId") int userId) {
        oAuthGroupService.addUserToGroup(groupId, userId);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{groupId}/users/{userId}")
    public Response removeUserFromGroup(@PathParam("groupId") int groupId, @PathParam("userId") int userId) {
        oAuthGroupService.removeUserFromGroup(groupId, userId);
        return Response.ok().build();
    }

    @POST
    @Path("/{groupId}/datasets/{datasetId}")
    public Response addDatasetToGroup(@PathParam("groupId") int groupId, @PathParam("datasetId") String datasetId) {
        UUID uuid = UUID.fromString(datasetId);
        oAuthGroupService.addDatasetToGroup(groupId, uuid);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{groupId}/datasets/{datasetId}")
    public Response removeDatasetFromGroup(@PathParam("groupId") int groupId, @PathParam("datasetId") String datasetId) {
        UUID uuid = UUID.fromString(datasetId);
        oAuthGroupService.removeDatasetFromGroup(groupId, uuid);
        return Response.ok().build();
    }

    @PUT
    @Path("/{groupId}/permission")
    public Response changeGroupPermission(@PathParam("groupId") int groupId, String permissionType) {
        OAuthGroup group = oAuthGroupService.getOAuthGroupById(groupId);
        if (group != null) {
            group.setPermisionType(PermissionType.fromString(permissionType));
            oAuthGroupService.updateOAuthGroup(group);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
