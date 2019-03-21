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

package org.springframework.data.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

public class ReadOnlyOneToNRelationshipFieldAccessorFactory<T extends NodeBacked, TARGET extends NodeBacked> extends NodeRelationshipFieldAccessorFactory<T, TARGET> {

	public ReadOnlyOneToNRelationshipFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
		super(graphDatabaseContext);
	}

	@Override
	public boolean accept(final Neo4JPersistentProperty f) {
	    if (!f.isRelationship()) return false;
        final RelationshipInfo info = f.getRelationshipInfo();
        return  info.isMultiple() && info.targetsNodes() && info.isReadonly();
	}

	@Override
	public FieldAccessor<T> forField(final Neo4JPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
        return new ReadOnlyOneToNRelationshipFieldAccessor<T, TARGET>(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<TARGET>) property.getRelationshipInfo().getTargetType().getType(), graphDatabaseContext,property);
	}

	public static class ReadOnlyOneToNRelationshipFieldAccessor<T extends NodeBacked, TARGET extends NodeBacked> extends OneToNRelationshipFieldAccessorFactory.OneToNRelationshipFieldAccessor<T, TARGET> {

		public ReadOnlyOneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<TARGET> elementClass, final GraphDatabaseContext graphDatabaseContext, Neo4JPersistentProperty field) {
	        super(type,direction,elementClass, graphDatabaseContext, field);
		}

	    @Override
	    public boolean isWriteable(T entity) {
	        return false;
	    }

	    @Override
		public Object setValue(final T entity, final Object newVal) {
			throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
		}

	}
}
