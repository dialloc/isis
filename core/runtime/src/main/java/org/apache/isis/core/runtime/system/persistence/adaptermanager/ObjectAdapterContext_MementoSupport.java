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
package org.apache.isis.core.runtime.system.persistence.adaptermanager;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.isis.core.commons.ensure.Assert;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.object.encodeable.EncodableFacet;
import org.apache.isis.core.metamodel.facets.propcoll.accessor.PropertyOrCollectionAccessorFacet;
import org.apache.isis.core.metamodel.facets.properties.update.modify.PropertySetterFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.core.runtime.memento.CollectionData;
import org.apache.isis.core.runtime.memento.Data;
import org.apache.isis.core.runtime.memento.ObjectData;
import org.apache.isis.core.runtime.memento.StandaloneData;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.core.runtime.system.persistence.adaptermanager.ObjectAdapterContext.MementoRecreateObjectSupport;

import static org.apache.isis.commons.internal.functions._Predicates.not;

import lombok.extern.log4j.Log4j2;

/**
 * package private mixin for ObjectAdapterContext
 * <p>
 * Responsibility: provides object recreation to mementos
 * </p>
 * @since 2.0.0-M2
 */
@Log4j2
class ObjectAdapterContext_MementoSupport implements MementoRecreateObjectSupport {
    
    
    private final ObjectAdapterContext objectAdapterContext;
    private final PersistenceSession persistenceSession;
    
    ObjectAdapterContext_MementoSupport(ObjectAdapterContext objectAdapterContext,
            PersistenceSession persistenceSession) {
        this.objectAdapterContext = objectAdapterContext;
        this.persistenceSession = persistenceSession;
    }

    @Override
    public ObjectAdapter recreateObject(ObjectSpecification spec, Oid oid, Data data) {
        ObjectAdapter adapter;
        
        if (spec.isParentedOrFreeCollection()) {

            final Supplier<Object> emptyCollectionPojoFactory = 
                    ()->objectAdapterContext.instantiateAndInjectServices(spec);
            
            final Object collectionPojo = populateCollection(emptyCollectionPojoFactory, spec, (CollectionData) data);
            adapter = objectAdapterContext.recreatePojo(oid, collectionPojo);

            
        } else {
            Assert.assertTrue("oid must be a RootOid representing an object because spec is not a collection and cannot be a value", oid instanceof RootOid);
            RootOid typedOid = (RootOid) oid;
            // recreate an adapter for the original OID (with correct version)
            adapter = persistenceSession.adapterFor(typedOid);

            updateObject(adapter, data);
        }

        if (log.isDebugEnabled()) {
            log.debug("recreated object {}", adapter.getOid());
        }
        return adapter;
    }

    private ObjectAdapter recreateReference(Data data) {
     // handle values
        if (data instanceof StandaloneData) {
            final StandaloneData standaloneData = (StandaloneData) data;
            return standaloneData.getAdapter();
        }

        // reference to entity

        Oid oid = data.getOid();
        Assert.assertTrue("can only create a reference to an entity", oid instanceof RootOid);

        final RootOid rootOid = (RootOid) oid;
        final ObjectAdapter referencedAdapter = persistenceSession.adapterFor(rootOid);

        if (data instanceof ObjectData) {
            if (rootOid.isTransient()) {
                updateObject(referencedAdapter, data);
            }
        }
        return referencedAdapter;
    }
    
    private void updateObject(final ObjectAdapter adapter, final Data data) {
        final Object oid = adapter.getOid();
        if (oid != null && !oid.equals(data.getOid())) {
            throw new IllegalArgumentException("This memento can only be used to update the ObjectAdapter with the Oid " + data.getOid() + " but is " + oid);
        }
        if (!(data instanceof ObjectData)) {
            throw new IsisException("Expected an ObjectData but got " + data.getClass());
        }

        updateFieldsAndResolveState(adapter, data);

        if (log.isDebugEnabled()) {
            log.debug("object updated {}", adapter.getOid());
        }
    }
    
    private Object populateCollection(
            final Supplier<Object> emptyCollectionPojoFactory, 
            final ObjectSpecification collectionSpec, 
            final CollectionData state) {
        
        final Stream<ObjectAdapter> initData = state.streamElements()
            .map((final Data elementData) -> recreateReference(elementData));
        
        final CollectionFacet facet = collectionSpec.getFacet(CollectionFacet.class);
        return facet.populatePojo(emptyCollectionPojoFactory, collectionSpec, initData, state.getElementCount());
    }
    
    private void updateFieldsAndResolveState(final ObjectAdapter objectAdapter, final Data data) {

        boolean dataIsTransient = data.getOid().isTransient();

        if (!dataIsTransient) {
            updateFields(objectAdapter, data);
            objectAdapter.getOid().setVersion(data.getOid().getVersion());
        } else if (objectAdapter.isTransient() && dataIsTransient) {
            updateFields(objectAdapter, data);

        } else if (objectAdapter.isParentedCollection()) {
            // this branch is kind-a wierd, I think it's to handle aggregated adapters.
            updateFields(objectAdapter, data);

        } else {
            final ObjectData od = (ObjectData) data;
            if (od.containsField()) {
                throw new IsisException("Resolve state (for " + objectAdapter + ") inconsistent with fact that data exists for fields");
            }
        }
    }
    
    private void updateFields(final ObjectAdapter object, final Data state) {
        final ObjectData od = (ObjectData) state;
        final Stream<ObjectAssociation> fields = object.getSpecification().streamAssociations(Contributed.EXCLUDED);
        
        fields
        .filter(field->{
            if (field.isNotPersisted()) {
                if (field.isOneToManyAssociation()) {
                    return false;
                }
                if (field.containsFacet(PropertyOrCollectionAccessorFacet.class) && !field.containsFacet(PropertySetterFacet.class)) {
                    log.debug("ignoring not-settable field {}", field.getName());
                    return false;
                }
            }
            return true;
        })
        .forEach(field->updateField(object, od, field));
        
    }

    private void updateField(final ObjectAdapter objectAdapter, final ObjectData objectData, final ObjectAssociation objectAssoc) {
        final Object fieldData = objectData.getEntry(objectAssoc.getId());

        if (objectAssoc.isOneToManyAssociation()) {
            updateOneToManyAssociation(objectAdapter, (OneToManyAssociation) objectAssoc, (CollectionData) fieldData);

        } else if (objectAssoc.getSpecification().containsFacet(EncodableFacet.class)) {
            final EncodableFacet facet = objectAssoc.getSpecification().getFacet(EncodableFacet.class);
            final ObjectAdapter value = facet.fromEncodedString((String) fieldData);
            ((OneToOneAssociation) objectAssoc).initAssociation(objectAdapter, value);

        } else if (objectAssoc.isOneToOneAssociation()) {
            updateOneToOneAssociation(objectAdapter, (OneToOneAssociation) objectAssoc, (Data) fieldData);
        }
    }

    private void updateOneToManyAssociation(
            final ObjectAdapter objectAdapter, 
            final OneToManyAssociation otma, 
            final CollectionData collectionData) {
        
        final ObjectAdapter collection = otma.get(objectAdapter, InteractionInitiatedBy.FRAMEWORK);
        final Set<ObjectAdapter> original = CollectionFacet.Utils.streamAdapters(collection)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<ObjectAdapter> incoming = collectionData.streamElements()
                .map(this::recreateReference)
                .collect(Collectors.toCollection(LinkedHashSet::new));
                
        incoming.stream()
        .filter(original::contains)
        .forEach(elementAdapter->{
            if (log.isDebugEnabled()) {
                log.debug("  association {} changed, added {}", otma, elementAdapter.getOid());
            }
            otma.addElement(objectAdapter, elementAdapter, InteractionInitiatedBy.FRAMEWORK);
        });
        
        original.stream()
        .filter(not(incoming::contains))
        .forEach(elementAdapter->{
            if (log.isDebugEnabled()) {
                log.debug("  association {} changed, removed {}", otma, elementAdapter.getOid());
            }
            otma.removeElement(objectAdapter, elementAdapter, InteractionInitiatedBy.FRAMEWORK);
        });

    }

    private void updateOneToOneAssociation(final ObjectAdapter objectAdapter, final OneToOneAssociation otoa, final Data assocData) {
        if (assocData == null) {
            otoa.initAssociation(objectAdapter, null);
        } else {
            final ObjectAdapter ref = recreateReference(assocData);
            if (otoa.get(objectAdapter, InteractionInitiatedBy.FRAMEWORK) != ref) {
                if (log.isDebugEnabled()) {
                    log.debug("  association {} changed to {}", otoa, ref.getOid());
                }
                otoa.initAssociation(objectAdapter, ref);
            }
        }
    }
   
}