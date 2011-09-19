/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.StartNode;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipNodeFieldAccessorFactory implements FieldAccessorFactory<RelationshipBacked> {

	private GraphDatabaseContext graphDatabaseContext;

    public RelationshipNodeFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
		super();
		this.graphDatabaseContext = graphDatabaseContext;
	}

	@Override
    public boolean accept(final Field f) {
        return isStartNodeField(f) || isEndNodeField(f);
    }

    private boolean isEndNodeField(final Field f) {
        return f.isAnnotationPresent(EndNode.class);
    }

    private boolean isStartNodeField(final Field f) {
        return f.isAnnotationPresent(StartNode.class);
    }

    @Override
    public FieldAccessor<RelationshipBacked> forField(final Field f) {
        if (isStartNodeField(f)) {
            return new RelationshipNodeFieldAccessor(f, graphDatabaseContext) {
                @Override
                protected Node getNode(final Relationship relationship) {
                    return relationship.getStartNode();
                }
            };

        }
        if (isEndNodeField(f)) {
            return new RelationshipNodeFieldAccessor(f, graphDatabaseContext) {
                @Override
                protected Node getNode(final Relationship relationship) {
                    return relationship.getEndNode();
                }
            };
        }
        return null;
    }

    public static abstract class RelationshipNodeFieldAccessor implements FieldAccessor<RelationshipBacked> {

        private final Field field;
        private final GraphDatabaseContext graphDatabaseContext;

        public RelationshipNodeFieldAccessor(final Field field, final GraphDatabaseContext graphDatabaseContext) {
            this.field = field;
            this.graphDatabaseContext = graphDatabaseContext;
        }

        @Override
        public Object setValue(final RelationshipBacked relationshipBacked, final Object newVal) {
            throw new InvalidDataAccessApiUsageException("Cannot change start or end node of existing relationship.");
        }

        @Override
        public Object getValue(final RelationshipBacked relationshipBacked) {
            final Relationship relationship = relationshipBacked.getPersistentState();
            final Node node = getNode(relationship);
            if (node == null) {
                return null;
            }
            final NodeBacked result = graphDatabaseContext.createEntityFromState(node, (Class<? extends NodeBacked>) field.getType());
            return doReturn(result);
        }

        protected abstract Node getNode(Relationship relationship);

        @Override
        public boolean isWriteable(final RelationshipBacked relationshipBacked) {
            return false;
        }
        
		@Override
		public Object getDefaultImplementation() {
			return null;
		}
    }

}
