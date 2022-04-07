/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.management;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import cz.it4i.fiji.datastore.security.Authorization;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Authorization
@Path("/")
@ApplicationScoped
public class DataServerManagerEndpoint {

	@Inject
	DataServerManager dataServerManager;

	@POST
	@Path("/stop")
	public Response stopDataServer() {
		log.debug("Stop was requested as REST request");
		dataServerManager.stopCurrentDataServer();
		return Response.ok().build();
	}
}
