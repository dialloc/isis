/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.metamodel.services.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.security.RoleMemento;
import org.apache.isis.applib.security.UserMemento;
import org.apache.isis.applib.services.sudo.SudoService;
import org.apache.isis.applib.services.user.UserService;
import org.apache.isis.core.commons.collections.Can;
import org.apache.isis.core.security.authentication.AuthenticationSessionProvider;

import lombok.Value;
import lombok.val;

@Service
@Named("isisMetaModel.UserServiceDefault")
@Order(OrderPrecedence.MIDPOINT)
@Primary
@Qualifier("Default")
public class UserServiceDefault implements UserService {
    
    @Inject private AuthenticationSessionProvider authenticationSessionProvider;
    
    @Service
    @Named("isisMetaModel.UserServiceDefault.SudoServiceSpi")
    @Order(OrderPrecedence.MIDPOINT)
    @Qualifier("UserServiceDefault")
    public static class SudoServiceSpi implements SudoService.Spi {

        @Inject UserServiceDefault userServiceDefault;
        
        @Override
        public void runAs(final String username, final List<String> roles) {
            userServiceDefault.overrideUserAndRoles(username, Can.ofCollection(roles));
        }

        @Override
        public void releaseRunAs() {
            userServiceDefault.resetOverrides();
        }
        
    }
    
    @PreDestroy
    public void cleanUp() {
        overrides.remove();
    }

    @Override
    public UserMemento getUser() {

        final UserAndRoleOverrides userAndRoleOverrides = currentOverridesIfAny();

        if (userAndRoleOverrides != null) {

            final String username = userAndRoleOverrides.user;

            final Can<String> roles;
            if (userAndRoleOverrides.roles != null) {
                roles = userAndRoleOverrides.roles;
            } else {
                // preserve the roles if were not overridden
                roles = previousRoles();
            }

            final List<RoleMemento> roleMementos = asRoleMementos(roles);
            return new UserMemento(username, roleMementos);

        } else {
            val authenticationSession =
                    authenticationSessionProvider.getAuthenticationSession();
            return authenticationSession.createUserMemento();
        }
    }


    @Value
    public static class UserAndRoleOverrides {
        final String user;
        final Can<String> roles;
    }


    /**
     * Not API; for use by the implementation of sudo/runAs (see {@link SudoService} etc.
     */
    public UserAndRoleOverrides currentOverridesIfAny() {
        final Stack<UserAndRoleOverrides> userAndRoleOverrides = overrides.get();
        return !userAndRoleOverrides.empty()
                ? userAndRoleOverrides.peek()
                        : null;
    }
    
    // -- HELPER
    
    private Can<String> previousRoles() {
        val authenticationSession =
                authenticationSessionProvider.getAuthenticationSession();
        val roles = authenticationSession.getRoles();
        return roles;
    }

    private Can<String> inheritRoles() {
        final UserAndRoleOverrides currentOverridesIfAny = currentOverridesIfAny();
        return currentOverridesIfAny != null
                ? currentOverridesIfAny.getRoles()
                        : authenticationSessionProvider.getAuthenticationSession().getRoles();
    }

    private static List<RoleMemento> asRoleMementos(final Can<String> roles) {
        final List<RoleMemento> mementos = new ArrayList<RoleMemento>();
        if (roles != null) {
            for (final String role : roles) {
                mementos.add(new RoleMemento(role));
            }
        }
        return mementos;
    }
    
    private final ThreadLocal<Stack<UserAndRoleOverrides>> overrides = ThreadLocal.withInitial(Stack::new);

    private void overrideUserAndRoles(final String user, final Can<String> rolesIfAny) {
        val roles = rolesIfAny.isNotEmpty() ? rolesIfAny : inheritRoles();
        this.overrides.get().push(new UserAndRoleOverrides(user, roles));
    }

    private void resetOverrides() {
        this.overrides.get().pop();
    }
    
    

}
