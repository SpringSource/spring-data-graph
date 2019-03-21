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

package org.springframework.data.neo4j.rest.index;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.springframework.data.neo4j.rest.RestGraphDatabase;
import org.springframework.data.neo4j.rest.RestRelationship;
import org.springframework.data.neo4j.rest.RestRequest;

import java.util.Map;

/**
 * @author mh
 * @since 24.01.11
 */
public class RestRelationshipIndex extends RestIndex<Relationship> implements RelationshipIndex {
    public RestRelationshipIndex( RestRequest restRequest, String indexName, RestGraphDatabase restGraphDatabase ) {
        super( restRequest, indexName, restGraphDatabase );
    }

    public Class<Relationship> getEntityType() {
        return Relationship.class;
    }

//    public void remove(Relationship entity, String key) {
//        throw new UnsupportedOperationException();
//    }
//
//    public void remove(Relationship entity) {
//        throw new UnsupportedOperationException();
//    }

    protected Relationship createEntity( Map<?, ?> item ) {
        return new RestRelationship( (Map<?, ?>) item, restGraphDatabase );
    }

    public org.neo4j.graphdb.index.IndexHits<Relationship> get( String s, Object o, Node node, Node node1 ) {
        throw new UnsupportedOperationException();
    }

    public org.neo4j.graphdb.index.IndexHits<Relationship> query( String s, Object o, Node node, Node node1 ) {
        throw new UnsupportedOperationException();
    }

    public org.neo4j.graphdb.index.IndexHits<Relationship> query( Object o, Node node, Node node1 ) {
        throw new UnsupportedOperationException();
    }
}
