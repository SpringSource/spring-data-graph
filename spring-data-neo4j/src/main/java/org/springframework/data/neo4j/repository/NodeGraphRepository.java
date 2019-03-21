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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

public class NodeGraphRepository<T extends NodeBacked> extends AbstractGraphRepository<Node, T> implements GraphRepository<T> {

    public NodeGraphRepository(final Class<T> clazz, final GraphDatabaseContext graphDatabaseContext) {
        super(graphDatabaseContext, clazz);
    }

    @Override
    protected Node getById(long id) {
        return graphDatabaseContext.getNodeById(id);
    }

    @Override
    public <N extends NodeBacked> Iterable<T> findAllByTraversal(final N startNode, final TraversalDescription traversalDescription) {
        return (Iterable<T>) startNode.findAllByTraversal((Class<? extends NodeBacked>) clazz, traversalDescription);
    }

    @Override
    public T save(T entity) {
        return (T) ((NodeBacked)entity).persist();
    }

    @Override
    public Iterable<T> save(Iterable<? extends T> entities) {
        for (T entity : entities) {
            save(entity);
        }
        return (Iterable<T>) entities;
    }
}

