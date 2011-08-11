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

package org.springframework.data.neo4j.config;

import javax.persistence.EntityManagerFactory;
import javax.validation.Validator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.Neo4jNodeBacking;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.node.PartialNodeEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.Neo4jRelationshipBacking;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;
import org.springframework.data.neo4j.template.Neo4jExceptionTranslator;
import org.springframework.data.neo4j.transaction.ChainedTransactionManager;
import org.springframework.data.persistence.EntityInstantiator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

/**
 * Abstract base class for code based configuration of Spring managed Neo4j infrastructure.
 * <p>Subclasses are required to provide an implementation of graphDbService ....
 * 
 * @author Thomas Risberg
 */
@Configuration
public class Neo4jConfiguration {
    private GraphDatabaseService graphDatabaseService;

    @Autowired(required = false)
    private Validator validator;

    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }


    @Autowired
    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    private EntityManagerFactory entityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Qualifier("&entityManagerFactory")
    @Autowired(required = false)
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public boolean isUsingCrossStorePersistence() {
        return entityManagerFactory != null;
    }

	@Bean
	public GraphDatabaseContext graphDatabaseContext() throws Exception {
        EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator = graphRelationshipInstantiator();
        EntityInstantiator<NodeBacked, Node> graphEntityInstantiator = graphEntityInstantiator();

        TypeRepresentationStrategyFactory typeRepresentationStrategyFactory =
                new TypeRepresentationStrategyFactory(graphDatabaseService, graphEntityInstantiator, relationshipEntityInstantiator);

        GraphDatabaseContext gdc = new GraphDatabaseContext();
        gdc.setGraphDatabaseService(getGraphDatabaseService());
        gdc.setConversionService(conversionService());
        gdc.setNodeTypeRepresentationStrategy(typeRepresentationStrategyFactory.getNodeTypeRepresentationStrategy());
        gdc.setRelationshipTypeRepresentationStrategy(typeRepresentationStrategyFactory.getRelationshipTypeRepresentationStrategy());
        if (validator!=null) {
            gdc.setValidator(validator);
        }
		return gdc;
	}

    @Bean
    protected ConversionService conversionService() throws Exception {
        return new Neo4jConversionServiceFactoryBean().getObject();
    }

    @Bean
    protected RelationshipEntityInstantiator graphRelationshipInstantiator() {
        return new RelationshipEntityInstantiator();
    }

    @Bean
	protected EntityInstantiator<NodeBacked, Node> graphEntityInstantiator() {
		if (isUsingCrossStorePersistence()) {
			return new PartialNodeEntityInstantiator(new NodeEntityInstantiator(), entityManagerFactory);
		} else {
			return new NodeEntityInstantiator();
		}
	}

	@Bean
	public DirectGraphRepositoryFactory directGraphRepositoryFactory() throws Exception {
		return new DirectGraphRepositoryFactory(graphDatabaseContext());
	}
	
	@Bean
	public Neo4jRelationshipBacking neo4jRelationshipBacking() throws Exception {
		Neo4jRelationshipBacking aspect = Neo4jRelationshipBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext());
        RelationshipEntityStateFactory entityStateFactory = relationshipEntityStateFactory();
		aspect.setRelationshipEntityStateFactory(entityStateFactory);
		return aspect;
	}

    @Bean
    public RelationshipEntityStateFactory relationshipEntityStateFactory() throws Exception {
        RelationshipEntityStateFactory entityStateFactory = new RelationshipEntityStateFactory();
        entityStateFactory.setGraphDatabaseContext(graphDatabaseContext());
        entityStateFactory.setRelationshipDelegatingFieldAccessorFactory(relationshipDelegatingFieldAccessorFactory());
        return entityStateFactory;
    }

    @Bean
	public Neo4jNodeBacking neo4jNodeBacking() throws Exception {
		Neo4jNodeBacking aspect = Neo4jNodeBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext());
        NodeEntityStateFactory entityStateFactory = nodeEntityStateFactory();
		aspect.setNodeEntityStateFactory(entityStateFactory);
		return aspect;
	}

    @Bean
    public NodeEntityStateFactory nodeEntityStateFactory() throws Exception {

        NodeEntityStateFactory entityStateFactory = new NodeEntityStateFactory();
        entityStateFactory.setGraphDatabaseContext(graphDatabaseContext());
        entityStateFactory.setEntityManagerFactory(entityManagerFactory);
        entityStateFactory.setNodeDelegatingFieldAccessorFactory(nodeDelegatingFieldAccessorFactory());
        return entityStateFactory;
    }

    @Bean
    public DelegatingFieldAccessorFactory<NodeBacked> nodeDelegatingFieldAccessorFactory() throws Exception {
        return new NodeDelegatingFieldAccessorFactory(graphDatabaseContext());
    }
    
    @Bean
    public DelegatingFieldAccessorFactory<RelationshipBacked> relationshipDelegatingFieldAccessorFactory() throws Exception {
        return new RelationshipDelegatingFieldAccessorFactory(graphDatabaseContext());
    }

    @Bean
	public PlatformTransactionManager transactionManager() {
		if (isUsingCrossStorePersistence()) {
			JpaTransactionManager jpaTm = new JpaTransactionManager(getEntityManagerFactory());
            JtaTransactionManager jtaTm = createJtaTransactionManager();
			return new ChainedTransactionManager(jpaTm, jtaTm);
		}
		else {
            return createJtaTransactionManager();
		}
	}

    private JtaTransactionManager createJtaTransactionManager() {
        JtaTransactionManager jtaTm = new JtaTransactionManager();
        final GraphDatabaseService gds = getGraphDatabaseService();
        if (gds instanceof AbstractGraphDatabase) {
            jtaTm.setTransactionManager(new SpringTransactionManager(gds));
            jtaTm.setUserTransaction(new UserTransactionImpl(gds));
        } else {
            final NullTransactionManager tm = new NullTransactionManager();
            jtaTm.setTransactionManager(tm);
            jtaTm.setUserTransaction(new UserTransactionAdapter(tm));
        }
        return jtaTm;
    }

    @Bean
    public ConfigurationCheck configurationCheck() throws Exception {
        return new ConfigurationCheck(graphDatabaseContext(),transactionManager());
    }

    @Bean
    public PersistenceExceptionTranslator persistenceExceptionTranslator() {
        return new Neo4jExceptionTranslator();
    }
}
