/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.form;

import io.jans.configapi.plugin.saml.model.TrustRelationship;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;


import java.io.InputStream;

import org.jboss.resteasy.annotations.providers.multipart.PartType;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;

public class KeycloakBrokerIdentityProviderForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
	@Valid
	@FormParam("trustRelationship")
	@PartType(MediaType.APPLICATION_JSON)
    private TrustRelationship trustRelationship;

    @NotNull
	@FormParam("metaDataFile")
	@PartType(MediaType.APPLICATION_OCTET_STREAM)
    private transient InputStream  metaDataFile;

    public TrustRelationship getTrustRelationship() {
        return trustRelationship;
    }

    public void setTrustRelationship(TrustRelationship trustRelationship) {
        this.trustRelationship = trustRelationship;
    }

    public InputStream getMetaDataFile() {
        return metaDataFile;
    }

    public void setMetaDataFile(InputStream metaDataFile) {
        this.metaDataFile = metaDataFile;
    }

    @Override
    public String toString() {
        return "KeycloakBrokerIdentityProviderForm ["
                + "trustRelationship=" + trustRelationship
                + "metaDataFile=" + metaDataFile 
                + "]";
    }

  
    
}
