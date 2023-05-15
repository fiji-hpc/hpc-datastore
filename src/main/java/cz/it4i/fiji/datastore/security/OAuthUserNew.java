package cz.it4i.fiji.datastore.security;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "oauth_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserNew extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String OAuthAlias;
    private String ClientToken;
    private String ClientID;
    private String ClientSecret;

    @Override
    public String toString() {
        String result=ClientToken+","+ClientID+","+ClientSecret;
        return result;
    }

}