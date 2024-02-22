package cz.it4i.fiji.datastore.security;

import java.util.Collection;
import java.util.LinkedList;

import cz.it4i.fiji.datastore.BaseEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;


@Log4j2
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class User{

	private static final long serialVersionUID = 1L;

	@OneToMany(cascade = CascadeType.ALL)
	private Collection<ACL> acl = new LinkedList<>();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String oauthAlias;
	private String clientToken;
	private String clientID;

	public User(Long id, Collection<ACL> acls) {
		this.id = id;
		acl = acls;
	}

	public void checkWriteAccess(String userID) {
		if (!acl.stream().allMatch(a -> a.isWrite())) {
			throw new UnauthorizedAccessException(userID);
		}
	}

}
