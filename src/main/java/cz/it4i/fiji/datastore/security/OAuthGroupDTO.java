package cz.it4i.fiji.datastore.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OAuthGroupDTO {
    private String name;
    private String ownerId;

}
