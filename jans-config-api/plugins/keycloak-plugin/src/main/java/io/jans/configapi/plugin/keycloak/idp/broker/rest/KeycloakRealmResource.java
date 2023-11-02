package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.plugin.keycloak.idp.broker.service.RealmService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;

import org.keycloak.representations.idm.RealmRepresentation;

@Path(Constants.KEYCLOAK + Constants.REALM_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeycloakRealmResource extends BaseResource {

    private static final String KC_REALM_DETAILS = "KC Realm Details";

    @Inject
    Logger logger;

    @Inject
    RealmService realmService;

    @Operation(summary = "Get all Keycloak realm", description = "Get all Keycloak realm.", operationId = "get-keycloak-realm", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RealmRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.KC_REALM_READ_ACCESS })
    public Response getAllKeycloakRealms() {
        List<RealmRepresentation> realms = realmService.getAllRealmDetails();
        logger.info("All realms:{}", realms);
        return Response.ok(realms).build();
    }

    @Operation(summary = "Get Keycloak realm by name", description = "Get Keycloak realm by name", operationId = "get-keycloak-realm-by-name", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.KC_REALM_READ_ACCESS })
    @Path(Constants.NAME_PATH + Constants.NAME_PATH_PARAM)
    public Response getKeycloakRealmByName(
            @Parameter(description = "name") @PathParam(Constants.NAME) @NotNull String name) {
        logger.info("Searching Keycloak Realm by name: {}", escapeLog(name));

        RealmRepresentation realmRepresentation = realmService.getRealmByName(name);

        logger.info("Keycloak realm found by name:{}, realmRepresentation:{}", name, realmRepresentation);

        return Response.ok(realmRepresentation).build();
    }

    @Operation(summary = "Create Keycloak realm", description = "Create Keycloak realm", operationId = "post-keycloak-realm", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_WRITE_ACCESS }))
    @RequestBody(description = "Keycloak realm", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class), examples = @ExampleObject(name = "Request example", value = "example/keycloak/keycloak-realm-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Keycloak realm", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @ProtectedApi(scopes = { Constants.KC_REALM_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.KC_REALM_WRITE_ACCESS })
    @POST
    public Response createNewKCRealm(@NotNull RealmRepresentation realmRepresentation, InputStream metadatafile)
            throws IOException {
        logger.info(" Create new KC realm - realmRepresentation:{} ", realmRepresentation);
        checkResourceNotNull(realmRepresentation, KC_REALM_DETAILS);
        realmRepresentation = this.realmService.createNewRealm(realmRepresentation);
        logger.info("Created new KC realm - realmRepresentation:{}", realmRepresentation);
        return Response.status(Response.Status.CREATED).entity(realmRepresentation).build();
    }

    @Operation(summary = "Update Keycloak realm", description = "Update Keycloak realm", operationId = "put-keycloak-realm", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_WRITE_ACCESS }))
    @RequestBody(description = "Keycloak realm", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class), examples = @ExampleObject(name = "Request example", value = "example/keycloak/keycloak-realm-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Keycloak realm object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @ProtectedApi(scopes = { Constants.KC_REALM_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.KC_REALM_WRITE_ACCESS })
    @POST
    public Response updateNewKCRealm(@NotNull RealmRepresentation realmRepresentation, InputStream metadatafile)
            throws IOException {
        logger.info(" Update KC realm - realmRepresentation:{} ", realmRepresentation);
        checkResourceNotNull(realmRepresentation, KC_REALM_DETAILS);
        realmRepresentation = this.realmService.updateRealm(realmRepresentation);
        logger.info("Updated KC realm - realmRepresentation:{}", realmRepresentation);
        return Response.status(Response.Status.OK).entity(realmRepresentation).build();
    }

    @Operation(summary = "Delete KC realm ", description = "Delete KC realm", operationId = "delete-keycloak-realm", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Path(Constants.NAME_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.KC_REALM_WRITE_ACCESS })
    @DELETE
    public Response deleteRealm(
            @Parameter(description = "Unique name of KC realm") @PathParam(Constants.NAME) @NotNull String name) {

        logger.info("Delete KC realm by name:{}", escapeLog(name));

        RealmRepresentation realmRepresentation = realmService.getRealmByName(name);
        if (realmRepresentation == null) {
            checkResourceNotNull(realmRepresentation, "KC relam does not exists by name - " + name);
        }
        realmService.deleteRealm(name);

        return Response.noContent().build();
    }

}
