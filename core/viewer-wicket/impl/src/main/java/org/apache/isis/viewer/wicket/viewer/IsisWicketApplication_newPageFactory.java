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

package org.apache.isis.viewer.wicket.viewer;

import org.apache.wicket.IPageFactory;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import org.apache.isis.commons.internal.base._Casts;
import org.apache.isis.commons.internal.exceptions._Exceptions;
import org.apache.isis.viewer.wicket.ui.pages.entity.EntityPage;

import lombok.RequiredArgsConstructor;

/**
 * 
 * Uses Wicket's default page factory, except for {@link EntityPage}s which require special instantiation:
 * <p> 
 * Constructor
 * {@link EntityPage#EntityPage(org.apache.isis.webapp.context.IsisWebAppCommonContext, PageParameters)}
 * needs the common-context as argument. 
 * 
 * @since 2.0
 *
 */
@RequiredArgsConstructor
class IsisWicketApplication_newPageFactory {

    private final IsisWicketApplication holder;

    public IPageFactory interceptPageFactory(IPageFactory delegate) {
        return new WebPageBaseFactory(holder, delegate);
    }

    @RequiredArgsConstructor
    static class WebPageBaseFactory implements IPageFactory {
        
        private final IsisWicketApplication holder;
        private final IPageFactory delegate;
        
        @Override
        public <C extends IRequestablePage> C newPage(Class<C> pageClass, PageParameters parameters) {
            
            if(EntityPage.class.equals(pageClass)) {
                return _Casts.uncheckedCast(EntityPage.bookmarked(holder.getCommonContext(), parameters));
            }
            
            return delegate.newPage(pageClass, parameters);
        }
        
        @Override
        public <C extends IRequestablePage> C newPage(Class<C> pageClass) {
            
            if(EntityPage.class.equals(pageClass)) {
                //TODO whenever this happens we should redirect to home
                throw _Exceptions.unexpectedCodeReach();
            }
            
            return delegate.newPage(pageClass);
        }
        
        @Override
        public <C extends IRequestablePage> boolean isBookmarkable(Class<C> pageClass) {
            
            if(EntityPage.class.equals(pageClass)) {
                return true;
            }
            
            return delegate.isBookmarkable(pageClass);
        }
    }
    
}
