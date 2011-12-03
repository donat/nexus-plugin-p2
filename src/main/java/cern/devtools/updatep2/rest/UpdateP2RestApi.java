/*
 * File: UpdateP2RestApi.java.
 * Created by Donat Csikos<dcsikos@cern.ch> at 3 Dec 2011.
 *
 * Copyright CERN 2011, All Rights Reserved.
 */
package cern.devtools.updatep2.rest;

import java.io.IOException;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

@SuppressWarnings("unused")
public class UpdateP2RestApi extends AbstractPlexusResource implements PlexusResource {

	@Override
	public Object getPayloadInstance() {
		return null;
	}

	@Override
	public PathProtectionDescriptor getResourceProtection() {
		return new PathProtectionDescriptor(this.getResourceUri(), "anon");
	}

	@Override
	public String getResourceUri() {
		return "/p2/refresh";
	}

	@Override
	public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
		try {
			new CompositeP2Updater("/opt/webservers/sonatype-work/nexus/storage/eclipse-plugins").processRoot();
			return "P2 update sites refreshed";
		} catch (Exception e) {
			return "P2 update process failed. Reason is: " + e.getMessage();
		}

	}
}