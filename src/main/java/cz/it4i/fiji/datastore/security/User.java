/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import java.util.Collection;
import java.util.LinkedList;

import cz.it4i.fiji.datastore.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
class User extends BaseEntity {


	private static final long serialVersionUID = 1L;

	@Builder.Default
	private Collection<ACL> acl = new LinkedList<>();

	public User(Long id, Collection<ACL> acls) {
		super(id);
		acl = acls;
	}

	public void checkWriteAccess(String userID) {
		if (!acl.stream().allMatch(a -> a.isWrite())) {
			throw new UnauthorizedAccessException(userID);
		}

	}



}
