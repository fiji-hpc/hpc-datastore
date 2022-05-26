/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.security;

import static cz.it4i.fiji.datastore.security.Constants.SECURITY_TOKEN;

import java.lang.reflect.Parameter;
import java.util.Objects;

import javax.interceptor.InvocationContext;
import javax.persistence.Entity;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;

import cz.it4i.fiji.datastore.BaseEntity;
import cz.it4i.fiji.datastore.register_service.DatasetRegisterServiceEndpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Authentication extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Setter
	private String accessToken;

	private String userID;

	private User user;

	private OAuthServer server;

	/**
	 * @param requestContext
	 */
	public void processRequest(ContainerRequestContext requestContext) {
		// do nothing at this point
	}



	public void checkAuthorization(InvocationContext ctx) {
		String name = ctx.getMethod().getName();
		switch (name) {
			case "startDatasetServer":
				checkStartDatasetServer(ctx);
				break;
			case "deleteDatasetVersions":
			case "deleteDatasetVersions_viaGet":
				checkDeleteDatasetVersions(ctx);
				break;
			case "writeBlock":
			case "rebuild":
			case "deleteDataset":
			case "deleteDataset_viaGet":
			case "setCommonMetadata":
			case "stopDataServer":
				user.checkWriteAccess(userID);
				break;
			case "createEmptyDataset":
			case "queryDataset":
			case "getCommonMetadata":
				break;
			default:
				throw new UnauthorizedAccessException(userID);
		}
	}

	public String getDataserverPropertyProperty() {
		return String.format("-D%s=%s", SECURITY_TOKEN, getAccessToken());
	}



	private void checkDeleteDatasetVersions(InvocationContext ctx) {
		String version = null;
		String versions = null;
		int i = 0;
		for (Parameter par : ctx.getMethod().getParameters()) {
			PathParam pathParam = par.getAnnotation(PathParam.class);
			if (pathParam != null) {
				if (pathParam.value().equals(
					DatasetRegisterServiceEndpoint.VERSION_PARAM))
				{
					version = (String) ctx.getParameters()[i];
				}
				else if (pathParam.value().equals(
					DatasetRegisterServiceEndpoint.VERSION_PARAMS))
				{
					versions = (String) ctx.getParameters()[i];
				}
			}

			i++;
		}
		for (int ver : DatasetRegisterServiceEndpoint.getVersions(version,
			versions))
		{
			if (ver == 0) {
				user.checkWriteAccess(userID);
			}
		}
	}

	private void checkStartDatasetServer(InvocationContext ctx) {
		int i = 0;
		String version = null;
		String mode = null;
		for (Parameter par : ctx.getMethod().getParameters()) {
			PathParam pathParam = par.getAnnotation(PathParam.class);
			if (pathParam != null) {
				if (pathParam.value().equals(
					DatasetRegisterServiceEndpoint.VERSION_PARAM))
				{
					version = (String) ctx.getParameters()[i];
				} else if (pathParam.value().equals(
					DatasetRegisterServiceEndpoint.MODE_PARAM))
				{
					mode = (String) ctx.getParameters()[i];
				}
			}

			i++;
		}
		if (Objects.equals(version, "0") && Objects.equals(mode, "write") ||
			version == null && mode == null)
		{
			user.checkWriteAccess(userID);
		}
	}
}
