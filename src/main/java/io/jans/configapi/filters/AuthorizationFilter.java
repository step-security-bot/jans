/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.auth.AuthorizationService;
import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * @author Mougang T.Gasmyr
 */
@Provider
@ProtectedApi
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {
   
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    
    @Inject
    Logger log;

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private ResourceInfo resourceInfo;
    

    @Inject
    AuthorizationService authorizationService;

    public void filter(ContainerRequestContext context) {
        System.out.println("=======================================================================");
        System.out.println("====== info.getAbsolutePath() = " +info.getAbsolutePath()+" , info.getRequestUri() = "+info.getRequestUri()+"\n\n");
        System.out.println("====== info.getBaseUri()=" + info.getBaseUri() + " info.getPath()=" + info.getPath() + " info.toString()=" + info.toString());
        System.out.println("====== request.getContextPath()=" + request.getContextPath() + " request.getRequestURI()=" + request.getRequestURI()+ " request.toString() " + request.toString());
        System.out.println("======" + context.getMethod() + " " + info.getPath() + " FROM IP " + request.getRemoteAddr());
        System.out.println("======PERFORMING AUTHORIZATION=========================================");
        String authorizationHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);

        System.out.println("\n\n\n AuthorizationFilter::filter() - authorizationHeader = " + authorizationHeader + "\n\n\n");

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(context);
            System.out.println("======ONLY TOKEN BASED AUTHORIZATION IS SUPPORTED======================");
            return;
        }
        try {
            this.authorizationService.processAuthorization(authorizationHeader, resourceInfo, context.getMethod(), request.getRequestURI());
            System.out.println("======AUTHORIZATION  GRANTED===========================================");
        } catch (Exception ex) {
            log.error("======AUTHORIZATION  FAILED ===========================================", ex);
            abortWithUnauthorized(context);
        }

    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME).build());
    }

}
