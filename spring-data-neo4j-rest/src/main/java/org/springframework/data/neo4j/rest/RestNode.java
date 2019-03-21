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

package org.springframework.data.neo4j.rest;


import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.helpers.collection.CombiningIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;

public class RestNode extends RestEntity implements Node {
    public RestNode( URI uri, RestGraphDatabase graphDatabase ) {
        super( uri, graphDatabase );
    }

    public RestNode( String uri, RestGraphDatabase graphDatabase ) {
        super( uri, graphDatabase );
    }

    public RestNode( Map<?, ?> data, RestGraphDatabase graphDatabase ) {
        super( data, graphDatabase );
    }

    public Relationship createRelationshipTo( Node toNode, RelationshipType type ) {
        return RestRelationship.create(this,(RestNode)toNode,type,null);
    }

    public Iterable<Relationship> getRelationships() {
        return wrapRelationships( restRequest.get( "relationships/all" ) );
    }

    @SuppressWarnings("unchecked")
    private Iterable<Relationship> wrapRelationships( RequestResult requestResult) {
        return new IterableWrapper<Relationship, Object>(
                (Collection<Object>) restRequest.toEntity(requestResult) ) {
            @Override
            protected Relationship underlyingObjectToObject( Object data ) {
                return new RestRelationship( (Map<?, ?>) data, getRestGraphDatabase() );
            }
        };
    }

    public Iterable<Relationship> getRelationships( RelationshipType... types ) {
        String path = getStructuralData().get( "all_relationships" ) + "/";
        int counter = 0;
        for ( RelationshipType type : types ) {
            if ( counter++ > 0 ) {
                path += "&";
            }
            path += type.name();
        }
        return wrapRelationships( restRequest.get( path ) );
    }


    enum RestDirection {
        INCOMING( Direction.INCOMING, "incoming", "in" ),
        OUTGOING( Direction.OUTGOING, "outgoing", "out" ),
        BOTH( Direction.BOTH, "all", "all" );

        public final Direction direction;
        public final String dataName;
        public final String pathName;

        RestDirection( Direction direction, String dataName, String pathName ) {
            this.direction = direction;
            this.dataName = dataName;
            this.pathName = pathName;
        }

        static RestDirection from( Direction direction ) {
            for ( RestDirection restDirection : values() ) {
                if ( restDirection.direction == direction ) return restDirection;
            }
            throw new RuntimeException( "No Rest-Direction for " + direction );
        }
    }

    public Iterable<Relationship> getRelationships( Direction direction ) {
        return wrapRelationships( restRequest.get( "relationships/" + RestDirection.from( direction ).pathName ) );
    }

    @Override
    public Iterable<Relationship> getRelationships(final Direction direction, RelationshipType... types) {
        return new CombiningIterable<Relationship>(new IterableWrapper<Iterable<Relationship>, RelationshipType>(asList(types)) {
            @Override
            protected Iterable<Relationship> underlyingObjectToObject(RelationshipType relationshipType) {
                return getRelationships(relationshipType,direction);
            }
        });
    }

    @Override
    public boolean hasRelationship(Direction direction, RelationshipType... types) {
        for (RelationshipType relationshipType : types) {
            if (hasRelationship(relationshipType, direction)) return true;
        }
        return false;
    }

    public Iterable<Relationship> getRelationships( RelationshipType type,
                                                    Direction direction ) {
        Object relationship = getStructuralData().get(directionParameter(direction));
        return wrapRelationships( restRequest.get( relationship + "/" + type.name() ) );
    }

    private String directionParameter(Direction direction) {
        return RestDirection.from(direction).dataName + "_relationships";
    }

    public Relationship getSingleRelationship( RelationshipType type,
                                               Direction direction ) {
        return IteratorUtil.singleOrNull( getRelationships( type, direction ) );
    }

    public boolean hasRelationship() {
        return getRelationships().iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType... types ) {
        return getRelationships( types ).iterator().hasNext();
    }

    public boolean hasRelationship( Direction direction ) {
        return getRelationships( direction ).iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType type, Direction direction ) {
        return getRelationships( type, direction ).iterator().hasNext();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, Object... rels ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction,
                               RelationshipType secondType, Direction secondDirection ) {
        throw new UnsupportedOperationException();
    }
}
