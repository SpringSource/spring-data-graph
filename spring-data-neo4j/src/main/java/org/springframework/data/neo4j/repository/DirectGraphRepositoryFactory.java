/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository;

import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

/**
 * Simple Factory for {@link NodeGraphRepository} instances.
 */
public class DirectGraphRepositoryFactory {

    private final GraphDatabaseContext graphDatabaseContext;

    public DirectGraphRepositoryFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @SuppressWarnings({"unchecked"})
    public <T extends GraphBacked<?>> GraphRepository<T> createGraphRepository(Class<T> clazz) {
        if (NodeBacked.class.isAssignableFrom(clazz)) return new NodeGraphRepository(clazz, graphDatabaseContext);
        if (RelationshipBacked.class.isAssignableFrom(clazz)) return new RelationshipGraphRepository(clazz, graphDatabaseContext);
        throw new IllegalArgumentException("Can't create graph repository for non graph entity of type "+clazz);
    }
}
