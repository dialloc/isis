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
package org.apache.isis.core.config.unittestsupport.internal;

import java.util.Map;

import org.apache.isis.core.commons.internal.collections._Maps;

import lombok.val;

final class _Config_trim {

    /**
     * Given {@code map}, for every key trim the corresponding value. 
     * Filter any absent values, but keep empty Strings.
     * @param map
     */
    static Map<String, String> trim(Map<String, String> map) {

        val trimmed = _Maps.<String, String> newHashMap();

        map.forEach((k, v)->{

            if(v!=null) {
                trimmed.put(k, v.trim());
            }

        });

        return trimmed;
    }

}
