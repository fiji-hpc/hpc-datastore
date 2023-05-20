package cz.it4i.fiji.datastore.security;

import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.security.ACL;
import cz.it4i.fiji.datastore.security.PermissionType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class OAuthGroup  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "owner_id")
    private User owner;
    @OneToMany(cascade = CascadeType.ALL)
    private List<User> users;
    @OneToMany(cascade = CascadeType.ALL)
    private List<Dataset> datasets;
    @OneToOne(cascade = CascadeType.ALL)
    private ACL acl;
    @Convert(converter = PermissionTypeSetConverter.class)
    private EnumSet<PermissionType> permissionType;

    public void addUser(User user) {
        if(users==null)
        {
            users=new ArrayList<>();
        }
        users.add(user);
    }

    public void addDataset(Dataset dataset) {
        if(datasets==null)
        {
            datasets=new ArrayList<>();
        }
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

    public boolean removeUser(User user) {
        if(users==null)
        {
            users=new ArrayList<>();
            return false;
        }
        if (users.contains(user)) {
            users.remove(user);
        } else {
            return false;
        }
        return true;
    }
}
