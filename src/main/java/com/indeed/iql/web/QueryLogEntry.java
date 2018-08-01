/*
 * Copyright (C) 2018 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.indeed.iql.web;

import com.google.common.base.Joiner;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class QueryLogEntry implements Iterable<Entry<String,String>> {
    private Map<String, String> propertyMap;

    public QueryLogEntry() {
        this.propertyMap = new LinkedHashMap<String, String>();
    }

    public void setProperty(String key, String val) {
        propertyMap.put(key, val);
    }

    public void setProperty(String key, long val) {
        propertyMap.put(key, Long.toString(val));
    }

    public void setProperty(String key, int val) {
        propertyMap.put(key, Integer.toString(val));
    }

    public void setProperty(String key, Set<String> val) {
        propertyMap.put(key, Joiner.on(",").join(val));
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        return propertyMap.entrySet().iterator();
    }

    @Override
    public String toString() {
        return Joiner.on(" ").withKeyValueSeparator(":").join(propertyMap);
    }
}