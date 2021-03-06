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
package org.apache.isis.applib.services.repository;

/**
 * TODO lower case enums not up to standard, is this an issue?
 * 
 * @apiNote use the provided predicates rather then directly referencing the enum names
 *  
 * @since 2.0
 */
public enum EntityState {

    not_Persistable,
    persistable_Attached,
    persistable_Detached, 
    persistable_Destroyed

    ;

    public boolean isPersistable() {
        return this != not_Persistable;
    }

    public boolean isAttached() {
        return this == persistable_Attached;
    }

    public boolean isDetached() {
        return this == persistable_Detached;
    }

    public boolean isDestroyed() {
        return this == persistable_Destroyed;
    }

}
