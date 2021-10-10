/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cc.ikey.playground.agentbuddy.sdk.weakconcurrent;

import java.util.Map;

public interface WeakMap<K, V> extends Iterable<Map.Entry<K, V>> {

    V get(K key);

    V put(K key, V value);

    V remove(K key);

    boolean containsKey(K process);

    void clear();

    V putIfAbsent(K key, V value);

    int approximateSize();

    interface DefaultValueSupplier<K, V> {
        V getDefaultValue(K key);
    }
}