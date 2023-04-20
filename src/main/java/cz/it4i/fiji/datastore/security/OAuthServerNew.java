package cz.it4i.fiji.datastore.security;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "oauth_server")
@Getter
@Setter
public class OAuthServerNew extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alias;

    private String sub;

    private String link_oauth;

    private String link_user_info;

    private String link_token;

    public OAuthServerNew()
    {}

    public OAuthServerNew(String alias, String sub, String link_oauth, String link_user_info, String link_token) {
        this.alias = alias;
        this.sub = sub;
        this.link_oauth = link_oauth;
        this.link_user_info = link_user_info;
        this.link_token = link_token;
    }

    public String BCSerlialize(){
        String result=alias+","+sub+","+link_oauth+","+link_user_info+","+link_token;
        return result;
    }
}