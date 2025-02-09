package io.jans.kc.idp.broker.rest;

import io.jans.kc.idp.broker.util.Constants;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.servers.*;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath(Constants.KEYCLOAK)
@OpenAPIDefinition(info = @Info(title = "Jans Config API - Keycloak", version = "1.0.0", contact = @Contact(name = "Gluu Support", url = "https://support.gluu.org", email = "xxx@gluu.org"),

license = @License(name = "Apache 2.0", url = "https://github.com/JanssenProject/jans/blob/main/LICENSE")),

tags = { @Tag(name = "Jans - Keycloak SAML Identity Broker") },

servers = { @Server(url = "https://jans.io/", description = "The Jans server") })

@SecurityScheme(name = "oauth2", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(clientCredentials = @OAuthFlow(tokenUrl = "https://{op-hostname}/.../token", scopes = {
@OAuthScope(name = Constants.KC_SAML_IDP_READ_ACCESS, description = "View Jans Keycloak SAML Identity-broker related information"),
@OAuthcope(name = Constants.KC_SAML_IDP_WRITE_ACCESS, description = "Manage Jans Keycloak SAML Identity-broker related information")}
)))
public class KcIdentityBrokerApiApplication extends Application { 

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        classes.add(KcSAMLIdentityBrokerResource.class);
        
        return classes;
    }
}
