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

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexManager;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

public abstract class Neo4jHelper {

    public static void cleanDb(GraphDatabaseContext graphDatabaseContext) {
        cleanDb(graphDatabaseContext.getGraphDatabaseService());
    }

    public static void cleanDb(GraphDatabaseService graphDatabaseService) {

        Transaction tx = graphDatabaseService.beginTx();
        try {
            removeNodes(graphDatabaseService);
            clearIndex(graphDatabaseService);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private static void removeNodes(GraphDatabaseService graphDatabaseService) {
        Node refNode = graphDatabaseService.getReferenceNode();
        for (Node node : graphDatabaseService.getAllNodes()) {
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                rel.delete();
            }
        }
        for (Node node : graphDatabaseService.getAllNodes()) {
            if (!refNode.equals(node)) {
                node.delete();
            }
        }
    }

    private static void clearIndex(GraphDatabaseService gds) {
        IndexManager indexManager = gds.index();
        for (String ix : indexManager.nodeIndexNames()) {
            indexManager.forNodes(ix).delete();
        }
        for (String ix : indexManager.relationshipIndexNames()) {
            indexManager.forRelationships(ix).delete();
        }
    }
}
