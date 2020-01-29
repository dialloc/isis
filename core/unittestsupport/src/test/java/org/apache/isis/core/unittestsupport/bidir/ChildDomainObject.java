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
package org.apache.isis.core.unittestsupport.bidir;

import javax.jdo.annotations.PersistenceCapable;


@PersistenceCapable
public class ChildDomainObject implements Comparable<ChildDomainObject> {

    // {{ Index (property)
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }
    // }}


    // {{ Parent (property)
    private ParentDomainObject parent;

    public ParentDomainObject getParent() {
        return parent;
    }

    public void setParent(final ParentDomainObject parent) {
        this.parent = parent;
    }
    public void modifyParent(final ParentDomainObject parent) {
        ParentDomainObject currentParent = getParent();
        // check for no-op
        if (parent == null || parent.equals(currentParent)) {
            return;
        }
        // delegate to parent to associate
        parent.addToChildren(this);
    }

    public void clearParent() {
        ParentDomainObject currentParent = getParent();
        // check for no-op
        if (currentParent == null) {
            return;
        }
        // delegate to parent to dissociate
        currentParent.removeFromChildren(this);
    }
    // }}



    @Override
    public int compareTo(ChildDomainObject other) {
        return this.getIndex() - other.getIndex();
    }

}
