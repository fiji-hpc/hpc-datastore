package cz.it4i.fiji.datastore.security;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.ws.rs.NotFoundException;
import java.util.*;

@Entity
@Table(name = "oauth_group")
@Getter
@Setter
public class OAuthGroup extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private int ownerID;
    @ElementCollection
    private List<Integer> usersIDs;
    @ElementCollection
    private List<UUID> datasetUUIDs;
    private ACL acl;
    private PermissionType permisionType;

    public OAuthGroup(int id, String name, int owner) {
        this.id = id;
        this.name = name;
        this.ownerID = owner;
        acl.setWrite(true);
        usersIDs=new ArrayList<>();
        datasetUUIDs=new ArrayList<>();
        usersIDs.add(owner);
    }

    public OAuthGroup() {

    }

    public void addUser(int userID)
    {
        usersIDs.add(userID);
    }
    public void deleteUsers(int user)
    {
            if(usersIDs.contains(user)) {
                usersIDs.remove(user);
            }
            else
            {
                throw new NotFoundException();
            }
    }
    public void addDatasetbyUUID(UUID id)
    {
            datasetUUIDs.add(id);
    }
    public void deleteDatasetbyUUID(UUID id)
    {
            if(datasetUUIDs.contains(id)) {
                datasetUUIDs.remove(id);
            }
            else
            {
                throw new NotFoundException();
            }
    }
    public void changePermission(PermissionType type)
    {
        permisionType=type;
        if (type==PermissionType.RW || type==PermissionType.W)
        {
            acl.setWrite(true);
        }
        else{
            acl.setWrite(false);
        }
    }
    public ACL getACL(){return acl;};



}

