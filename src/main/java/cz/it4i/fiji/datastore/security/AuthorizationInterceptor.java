/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import lombok.extern.log4j.Log4j2;

@Interceptor
@Authorization
@Log4j2
class AuthorizationInterceptor {

	@Inject
	SecurityModule securityModule;

	@AroundInvoke
	public Object monitorInvocation(InvocationContext ctx) throws Exception {

		log.debug("Invocated for user {}", securityModule.getUserID());
		securityModule.checkAuthorization(ctx);
		return ctx.proceed();
	}
}
