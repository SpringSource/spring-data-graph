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

package org.springframework.data.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.Attribute;
import org.springframework.data.neo4j.Group;
import org.springframework.data.neo4j.*;
import org.springframework.data.neo4j.Person;
import org.springframework.data.neo4j.PersonRepository;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

    public class NodeEntityTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private DirectGraphRepositoryFactory graphRepositoryFactory;

	@Autowired
	private PersonRepository personRepository;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testUserConstructor() {
        Person p = persistedPerson("Rod", 39);
        assertEquals(p.getName(), p.getPersistentState().getProperty("name"));
        assertEquals(p.getAge(), p.getPersistentState().getProperty("age"));
        Person found = graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(p.getNodeId()), Person.class);
        assertEquals("Rod", found.getPersistentState().getProperty("name"));
        assertEquals(39, found.getPersistentState().getProperty("age"));
    }

    @Test
    @Transactional
    public void testSetProperties() {
        String name = "Michael";
        int age = 35;
        short height = 182;

        Person p = persistedPerson("Foo", 2);
        p.setName( name );
        p.setAge( age );
        p.setHeight( height );
        assertEquals( name, p.getPersistentState().getProperty( "name" ) );
        assertEquals( age, p.getPersistentState().getProperty("age"));
        assertEquals((Short)height, p.getHeight());
    }

    @Test
    @Transactional
    public void testSetShortProperty() {
        Person p = persistedPerson("Foo", 2);
        p.setHeight((short)182);
        assertEquals((Short)(short)182, p.getHeight());
        assertEquals((short)182, p.getPersistentState().getProperty("height"));
    }
    @Test
    @Transactional
    public void testSetShortNameProperty() {
        Group group = new Group().persist();
        group.setName("developers");
        assertEquals("developers", group.getPersistentState().getProperty("name"));
    }
    // own transaction handling because of https://neo4j.com/docs/
    @Test(expected = NotFoundException.class)
    public void testDeleteEntityFromGDC() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        graphDatabaseContext.removeNodeEntity(spouse);
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        Person spouseFromIndex = personRepository.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index",spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteEntity() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        spouse.remove();
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        Person spouseFromIndex = personRepository.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index", spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

    @Test
    public void testPersistGenericEntity() {
        final Attribute<String> attribute = new Attribute<String>();
        attribute.setValue("test");
        attribute.persist();
    }
}
