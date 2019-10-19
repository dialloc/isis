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

package org.apache.isis.viewer.wicket.ui.components.collectioncontents.ajaxtable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.commons.internal.base._NullSafe;
import org.apache.isis.commons.internal.collections._Lists;
import org.apache.isis.metamodel.adapter.ObjectAdapter;
import org.apache.isis.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.metamodel.consent.InteractionResult;
import org.apache.isis.metamodel.interactions.InteractionUtils;
import org.apache.isis.metamodel.interactions.ObjectVisibilityContext;
import org.apache.isis.metamodel.interactions.VisibilityContext;
import org.apache.isis.metamodel.spec.ManagedObject;
import org.apache.isis.metamodel.spec.ObjectSpecification;
import org.apache.isis.metamodel.spec.ObjectSpecificationException;
import org.apache.isis.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.viewer.wicket.model.models.EntityCollectionModel;
import org.apache.isis.viewer.wicket.model.models.EntityModel;

import lombok.val;

/**
 * Part of the {@link AjaxFallbackDefaultDataTable} API.
 */
public class CollectionContentsSortableDataProvider extends SortableDataProvider<ObjectAdapter,String> {

    private static final long serialVersionUID = 1L;

    private final EntityCollectionModel model;

    public CollectionContentsSortableDataProvider(final EntityCollectionModel model) {
        this.model = model;
    }

    @Override
    public IModel<ObjectAdapter> model(final ObjectAdapter adapter) {
        return EntityModel.ofAdapter(model.getCommonContext(), adapter);
    }

    @Override
    public long size() {
        return model.getObject().size();
    }

    @Override
    public void detach() {
        super.detach();
        model.detach();
    }

    public EntityCollectionModel getEntityCollectionModel() {
        return model;
    }

    @Override
    public Iterator<ObjectAdapter> iterator(final long first, final long count) {

        final List<ObjectAdapter> adapters = model.getObject();

        final List<ObjectAdapter> visibleAdapters =
                _Lists.filter(adapters, ignoreHidden());

        // need to create a list from the iterable, then back to an iterable
        // because guava's Ordering class doesn't support sorting of iterable -> iterable
        final List<ObjectAdapter> sortedVisibleAdapters = sortedCopy(visibleAdapters, getSort());
        final List<ObjectAdapter> pagedAdapters = subList(first, count, sortedVisibleAdapters);
        return pagedAdapters.iterator();
    }

    private static List<ObjectAdapter> subList(
            final long first,
            final long count,
            final List<ObjectAdapter> objectAdapters) {

        final int fromIndex = (int) first;
        // if adapters where filter out (as invisible), then make sure don't run off the end
        final int toIndex = Math.min((int) (first + count), objectAdapters.size());

        return objectAdapters.subList(fromIndex, toIndex);
    }

    private List<ObjectAdapter> sortedCopy(
            final List<ObjectAdapter> adapters,
            final SortParam<String> sort) {

        val copy = _Lists.newArrayList(adapters);
        
        final ObjectAssociation sortProperty = lookupAssociationFor(sort);
        if(sortProperty != null) {
            Collections.sort(copy, orderingBy(sortProperty, sort.isAscending()));
        }
        
        return copy;
    }

    private ObjectAssociation lookupAssociationFor(final SortParam<String> sort) {

        if(sort == null) {
            return null;
        }

        final ObjectSpecification elementSpec = model.getTypeOfSpecification();
        final String sortPropertyId = sort.getProperty();

        try {
            // might be null, or throw ex
            return elementSpec.getAssociation(sortPropertyId);
        } catch(ObjectSpecificationException ex) {
            // eg invalid propertyId
            return null;
        }
    }

    private Predicate<ObjectAdapter> ignoreHidden() {
        return new Predicate<ObjectAdapter>() {
            @Override
            public boolean test(ObjectAdapter input) {
                final InteractionResult visibleResult = InteractionUtils.isVisibleResult(input.getSpecification(), createVisibleInteractionContext(input));
                return visibleResult.isNotVetoing();
            }
        };
    }

    private VisibilityContext<?> createVisibleInteractionContext(final ObjectAdapter objectAdapter) {
        return new ObjectVisibilityContext(
                objectAdapter, objectAdapter.getSpecification().getIdentifier(), InteractionInitiatedBy.USER,
                Where.ALL_TABLES);
    }

    private static Comparator<ManagedObject> orderingBy(
            ObjectAssociation sortProperty, 
            boolean ascending) {
        
        return new Comparator<ManagedObject>(){

            @Override
            public int compare(ManagedObject p, ManagedObject q) {
                val pSort = sortProperty.get(p, InteractionInitiatedBy.FRAMEWORK);
                val qSort = sortProperty.get(q, InteractionInitiatedBy.FRAMEWORK);
                
                return (ascending ? NATURAL_NULL_FIRST : NATURAL_NULL_FIRST.reversed())
                        .compare(pSort, qSort);
            }
        };
        
    }

    private static final Comparator<ManagedObject> NATURAL_NULL_FIRST = new Comparator<ManagedObject>(){
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public int compare(ManagedObject p, ManagedObject q) {
            val pPojo = p.getPojo();
            val qPojo = q.getPojo();
            if(!(pPojo instanceof Comparable) || !(qPojo instanceof Comparable)) {
                return 0;
            }
            return _NullSafe.compareNullsFirst((Comparable)pPojo, (Comparable)qPojo);
        }
        
    };



}