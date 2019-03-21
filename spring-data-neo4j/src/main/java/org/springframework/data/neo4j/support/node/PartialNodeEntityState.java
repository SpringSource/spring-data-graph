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
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.fieldaccess.*;
import org.springframework.data.neo4j.mapping.Neo4JPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import javax.persistence.PersistenceUnitUtil;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class PartialNodeEntityState<ENTITY extends NodeBacked> extends DefaultEntityState<ENTITY, Node> {

    public static final String FOREIGN_ID = "foreignId";
    public static final String FOREIGN_ID_INDEX = "foreign_id";

    private final GraphDatabaseContext graphDatabaseContext;
    private PersistenceUnitUtil persistenceUnitUtil;

    public PartialNodeEntityState(final Node underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext, PersistenceUnitUtil persistenceUnitUtil, final PartialNodeDelegatingFieldAccessorFactory delegatingFieldAccessorFactory, final Neo4JPersistentEntity<ENTITY> persistentEntity) {
    	super(underlyingState, entity, type, delegatingFieldAccessorFactory, persistentEntity);
        this.graphDatabaseContext = graphDatabaseContext;
        this.persistenceUnitUtil = persistenceUnitUtil;
    }

    // TODO handle non persisted Entity like running outside of an transaction
    @Override
    public void createAndAssignState() {
        if (entity.getPersistentState() != null) return;
        try {
            final Object id = getId(entity);
            if (id == null) return;
            final String foreignId = createForeignId(id);
            IndexHits<Node> indexHits = getForeignIdIndex().get(FOREIGN_ID, foreignId);
            Node node = indexHits.hasNext() ? indexHits.next() : null;
            if (node == null) {
                node = graphDatabaseContext.createNode();
                persistForeignId(node, id);
                setPersistentState(node);
                log.info("User-defined constructor called on class " + entity.getClass() + "; created Node [" + entity.getPersistentState() + "]; Updating metamodel");
                graphDatabaseContext.postEntityCreation(node, type);
            } else {
                setPersistentState(node);
                entity.setPersistentState(node);
            }
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    @Override
    public ENTITY persist() {
        if (getPersistentState() == null) {
            createAndAssignState();
        }
        return entity;
    }

    @Override
    public boolean isWritable(Field field) {
        final FieldAccessor<ENTITY> accessor = accessorFor(property(field));
        if (accessor == null) return false; // difference to default behaviour, we don't care for non-managed fields here
        return accessor.isWriteable(entity);
    }

    private void persistForeignId(Node node, Object id) {
        if (!node.hasProperty(FOREIGN_ID) && id != null) {
            final String foreignId = createForeignId(id);
            node.setProperty(FOREIGN_ID, id);
            getForeignIdIndex().add(node, FOREIGN_ID, foreignId);
        }
    }

    private Index<Node> getForeignIdIndex() {
        return graphDatabaseContext.getIndex(type);
    }

    private String createForeignId(Object id) {
        return type.getName() + ":" + id;
    }

    public Object getId(final Object entity) {
        return persistenceUnitUtil!=null ? persistenceUnitUtil.getIdentifier(entity) : null;
    }

    public static class PartialNodeDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory {

        public PartialNodeDelegatingFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
            super(graphDatabaseContext);
        }

        @Override
        protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
            return Arrays.<FieldAccessorListenerFactory<?>>asList(
                    new IndexingPropertyFieldAccessorListenerFactory(
                            getGraphDatabaseContext(),
                            newPropertyFieldAccessorFactory(),
                            newConvertingNodePropertyFieldAccessorFactory()) {
                        @Override
                        public boolean accept(Neo4JPersistentProperty property) {
                            return property.isAnnotationPresent(GraphProperty.class) && super.accept(property);
                        }
                    },
                    new JpaIdFieldAccessListenerFactory());
        }

        @Override
        protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
            return Arrays.<FieldAccessorFactory<?>>asList(
                    //new IdFieldAccessorFactory(),
                    //new TransientFieldAccessorFactory(),
                    new TraversalFieldAccessorFactory(),
                    new QueryFieldAccessorFactory(),
                    newPropertyFieldAccessorFactory(),
                    newConvertingNodePropertyFieldAccessorFactory(),
                    new SingleRelationshipFieldAccessorFactory(getGraphDatabaseContext()) {
                        @Override
                        public boolean accept(Neo4JPersistentProperty property) {
                            return property.isAnnotationPresent(RelatedTo.class) && super.accept(property);
                        }
                    },
                    new OneToNRelationshipFieldAccessorFactory(getGraphDatabaseContext()),
                    new ReadOnlyOneToNRelationshipFieldAccessorFactory(getGraphDatabaseContext()),
                    new OneToNRelationshipEntityFieldAccessorFactory(getGraphDatabaseContext())
            );
        }

        private ConvertingNodePropertyFieldAccessorFactory newConvertingNodePropertyFieldAccessorFactory() {
            return new ConvertingNodePropertyFieldAccessorFactory(getGraphDatabaseContext().getConversionService()) {
                @Override
                public boolean accept(Neo4JPersistentProperty f) {
                    return f.isAnnotationPresent(GraphProperty.class) && super.accept(f);
                }
            };
        }

        private PropertyFieldAccessorFactory newPropertyFieldAccessorFactory() {
            return new PropertyFieldAccessorFactory(getGraphDatabaseContext().getConversionService()) {
                @Override
                public boolean accept(Neo4JPersistentProperty f) {
                    return f.isAnnotationPresent(GraphProperty.class) && super.accept(f);
                }
            };
        }
    }
}
