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

package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.Traversal;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.query.ConversionServiceQueryResultConverter;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.GremlinQueryEngine;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;

/**
 * @author mh
 * @since 29.03.11
 */
public class DelegatingGraphDatabase implements GraphDatabase {

    protected GraphDatabaseService delegate;
    private ConversionService conversionService;

    public DelegatingGraphDatabase(final GraphDatabaseService delegate) {
        this.delegate = delegate;
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Node getNodeById(long id) {
        return delegate.getNodeById(id);
    }

    @Override
    public Node createNode(Map<String, Object> props) {
        return setProperties(delegate.createNode(), props);
    }

    private <T extends PropertyContainer> T setProperties(T primitive, Map<String, Object> properties) {
        assert primitive != null;
        if (properties==null || properties.isEmpty()) return primitive;
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if (prop.getValue()==null) {
                primitive.removeProperty(prop.getKey());
            } else {
                primitive.setProperty(prop.getKey(), prop.getValue());
            }
        }
        return primitive;
    }

    @Override
    public Relationship getRelationshipById(long id) {
        return delegate.getRelationshipById(id);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props) {
        return setProperties(startNode.createRelationshipTo(endNode,type),props);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        IndexManager indexManager = delegate.index();
        if (indexManager.existsForNodes(indexName)) return (Index<T>) indexManager.forNodes(indexName);
        if (indexManager.existsForRelationships(indexName)) return (Index<T>) indexManager.forRelationships(indexName);
        throw new IllegalArgumentException("Index "+indexName+" does not exist.");
    }

    // TODO handle existing indexes
    @Override
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
        IndexManager indexManager = delegate.index();
        if (isNode(type)) {
            if (indexManager.existsForNodes(indexName))
                return (Index<T>) checkAndGetExistingIndex(indexName, fullText, indexManager.forNodes(indexName));
            return (Index<T>) indexManager.forNodes(indexName, indexConfigFor(fullText));
        } else {
            if (indexManager.existsForRelationships(indexName))
                return (Index<T>) checkAndGetExistingIndex(indexName, fullText, indexManager.forRelationships(indexName));
            return (Index<T>) indexManager.forRelationships(indexName, indexConfigFor(fullText));
        }
    }

    public boolean isNode(Class<? extends PropertyContainer> type) {
        if (type.equals(Node.class)) return true;
        if (type.equals(Relationship.class)) return false;
        throw new IllegalArgumentException("Unknown Graph Primitive, neither Node nor Relationship"+type);
    }

    private <T extends PropertyContainer> Index<T> checkAndGetExistingIndex(final String indexName, boolean fullText, final Index<T> index) {
        Map<String, String> existingConfig = delegate.index().getConfiguration(index);
        Map<String, String> config = indexConfigFor(fullText);
        if (config.equals(existingConfig)) return index;
        throw new IllegalArgumentException("Setup for index "+indexName+" does not match "+(fullText ? "fulltext":"exact"));
     }

    private Map<String, String> indexConfigFor(boolean fullText) {
        return fullText ? LuceneIndexImplementation.FULLTEXT_CONFIG : LuceneIndexImplementation.EXACT_CONFIG;
    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return Traversal.description();
    }

    public <T> QueryEngine<T> queryEngineFor(QueryType type) {
        switch (type) {
            case Cypher:  return (QueryEngine<T>)new CypherQueryEngine(delegate, createResultConverter());
            case Gremlin: return (QueryEngine<T>) new GremlinQueryEngine(delegate);
        }
        throw new IllegalArgumentException("Unknown Query Engine Type "+type);
    }

    private ConversionServiceQueryResultConverter createResultConverter() {
        if (conversionService == null) return null;
        return new ConversionServiceQueryResultConverter(conversionService);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public Node getReferenceNode() {
        return delegate.getReferenceNode();
    }
}
