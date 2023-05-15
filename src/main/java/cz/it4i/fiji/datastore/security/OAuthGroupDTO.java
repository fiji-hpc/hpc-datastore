package cz.it4i.fiji.datastore.security;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OAuthGroupDTO {
    private String name;
    private String ownerId;

    public OAuthGroupDTO() {
    }

    public OAuthGroupDTO(String name, String owner) {
        this.name = name;
        this.ownerId = owner;
    }

}
