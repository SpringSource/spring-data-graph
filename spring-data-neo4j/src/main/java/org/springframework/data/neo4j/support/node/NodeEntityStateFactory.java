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

package org.springframework.data.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.DetachedEntityState;
import org.springframework.data.neo4j.mapping.Neo4JMappingContext;
import org.springframework.data.neo4j.mapping.Neo4JPersistentEntity;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

public class NodeEntityStateFactory {

	private GraphDatabaseContext graphDatabaseContext;
	
    private EntityManagerFactory entityManagerFactory;

    private DelegatingFieldAccessorFactory<NodeBacked> nodeDelegatingFieldAccessorFactory;

    private PartialNodeEntityState.PartialNodeDelegatingFieldAccessorFactory delegatingFieldAccessorFactory;

    private Neo4JMappingContext mappingContext;

    public EntityState<NodeBacked,Node> getEntityState(final NodeBacked entity) {
        final Class<? extends NodeBacked> entityType = entity.getClass();
        final NodeEntity graphEntityAnnotation = entityType.getAnnotation(NodeEntity.class); // todo cache ??
        final Neo4JPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entityType);
        if (graphEntityAnnotation.partial()) {
            final PartialNodeEntityState<NodeBacked> partialNodeEntityState = new PartialNodeEntityState<NodeBacked>(null, entity, entityType, graphDatabaseContext, getPersistenceUnitUtils(), delegatingFieldAccessorFactory, (Neo4JPersistentEntity<NodeBacked>) persistentEntity);
            return new DetachedEntityState<NodeBacked, Node>(partialNodeEntityState, graphDatabaseContext) {
                @Override
                protected boolean isDetached() {
                    return super.isDetached() || partialNodeEntityState.getId(entity) == null;
                }
            };
        } else {
            NodeEntityState<NodeBacked> nodeEntityState = new NodeEntityState<NodeBacked>(null, entity, entityType, graphDatabaseContext, nodeDelegatingFieldAccessorFactory, (Neo4JPersistentEntity<NodeBacked>) persistentEntity);
            // alternative was return new NestedTransactionEntityState<NodeBacked, Node>(nodeEntityState,graphDatabaseContext);
            return new DetachedEntityState<NodeBacked, Node>(nodeEntityState, graphDatabaseContext);
        }
    }

    private PersistenceUnitUtil getPersistenceUnitUtils() {
        if (entityManagerFactory == null) return null;
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    public void setNodeDelegatingFieldAccessorFactory(
    		DelegatingFieldAccessorFactory<NodeBacked> nodeDelegatingFieldAccessorFactory) {
		this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
	}
	
	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public Neo4JMappingContext getMappingContext() {
        return mappingContext;
    }

    public void setMappingContext(Neo4JMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    @PostConstruct
    private void setUp() {
         this.delegatingFieldAccessorFactory = new PartialNodeEntityState.PartialNodeDelegatingFieldAccessorFactory(graphDatabaseContext);
    }
}
