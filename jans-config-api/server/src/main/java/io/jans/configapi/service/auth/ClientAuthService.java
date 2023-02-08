package io.jans.configapi.service.auth;

import io.jans.orm.search.filter.Filter;

//import ClientAuthorizationsService;
//import io.jans.as.server.model.ldap.ClientAuthorization;
import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ClientAuthService {

   //@Inject
   //ClientAuthorizationsService clientAuthorizationsService;
   
   @Inject
   Logger logger;
   
    public Map<Client, Set<Scope>> getUserAuthorizations(String userId) {
        
        logger.info(" Authorizations details to be fetched for userId:{} ",userId);
        /*List<ClientAuthorization> clientAuthorizationList = new ArrayList<>();
        clientAuthorizationList =  clientAuthorizationsService.find(userId);
        logger.debug("{} client-authorization entries found", clientAuthorizationList);
        return clientAuthorizationList;*/
        return null;

    }

    public void removeClientAuthorizations(String userId, String userName, String clientId) {

     
        logger.info("Removing client authorizations for user {}", userName);
        
        logger.info("Removing refresh tokens associated to this user/client pair");
        return;

    }

}
