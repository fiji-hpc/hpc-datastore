package cz.it4i.fiji.datastore.security;

import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.security.ACL;
import cz.it4i.fiji.datastore.security.PermissionType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Entity
@Table(name = "oauth_group")
@Getter
@Setter
public class OAuthGroup extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "owner_id")
    private OAuthUserNew owner;
    @OneToMany(cascade = CascadeType.ALL)
    private List<OAuthUserNew> users;
    @OneToMany(cascade = CascadeType.ALL)
    private List<Dataset> datasets;
    private ACL acl;
    @Convert(converter = PermissionTypeSetConverter.class)
    private EnumSet<PermissionType> permissionType;

    public OAuthGroup(int id, String name, OAuthUserNew owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        acl.setWrite(true);
        users = new ArrayList<>();
        datasets = new ArrayList<>();
        users.add(owner);
    }

    public OAuthGroup() {

    }

    public void addUser(OAuthUserNew user) {
        users.add(user);
    }

    public void deleteUser(OAuthUserNew user) {
        if (users.contains(user)) {
            users.remove(user);
        } else {
            throw new NotFoundException();
        }
    }

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
    }

    public void deleteDataset(Dataset dataset) {
        if (datasets.contains(dataset)) {
            datasets.remove(dataset);
        } else {
            throw new NotFoundException();
        }
    }

    public void changePermission(EnumSet<PermissionType> types) {
        permissionType = types;
        acl.setWrite(types.contains(PermissionType.W));
    }

    public ACL getACL() {
        return acl;
    }

    public void removeDataset(Dataset dataset) {
        if (datasets.contains(dataset)) {
            datasets.remove(dataset);
        } else {
            throw new NotFoundException();
        }
    }

    public boolean removeUser(OAuthUserNew user) {
        if (users.contains(user)) {
            users.remove(user);
        } else {
            return false;
        }
        return true;
    }
}
