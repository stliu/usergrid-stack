/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.IndexUpdate;
import org.usergrid.persistence.cassandra.IndexUpdate.IndexEntry;
import org.usergrid.persistence.cassandra.RelationManagerImpl;
import org.usergrid.utils.JsonUtils;
import org.usergrid.utils.UUIDUtils;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IndexTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory
			.getLogger(CollectionTest.class);

	public static final String[] alphabet = { "Alpha", "Bravo", "Charlie",
			"Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliet",
			"Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec",
			"Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey",
			"X-ray", "Yankee", "Zulu" }

	;

	@Test
	public void testCollectionOrdering() throws Exception {
		logger.info("testCollectionOrdering");

		UUID applicationId = createApplication("testOrganization",
				"testCollectionOrdering");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		for (int i = alphabet.length - 1; i >= 0; i--) {
			String name = alphabet[i];
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", name);

			em.create("item", properties);

		}

		int i = 0;

		Query query = Query.fromQL("order by name");
		Results r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}

		query = Query.fromQL("order by name").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}

		query = Query.fromQL("order by name").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}

		assertEquals(alphabet.length, i);

		i = alphabet.length;

		query = Query.fromQL("order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}

		query = Query.fromQL("order by name desc").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		// logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}

		query = Query.fromQL("order by name desc").withCursor(r.getCursor());
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}

		assertEquals(0, i);

	}

	@Test
	public void testCollectionFilters() throws Exception {
		logger.info("testCollectionFilters");

		UUID applicationId = createApplication("testOrganization",
				"testCollectionFilters");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		for (int i = alphabet.length - 1; i >= 0; i--) {
			String name = alphabet[i];
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", name);

			em.create("item", properties);

		}

		Query query = Query.fromQL("name < 'delta'");
		Results r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		int i = 0;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(3, i);

		query = Query.fromQL("name <= 'delta'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 0;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(4, i);

		query = Query.fromQL("name <= 'foxtrot' and name > 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 2;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(6, i);

		query = Query.fromQL("name < 'foxtrot' and name > 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 2;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(5, i);

		query = Query.fromQL("name < 'foxtrot' and name >= 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 1;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(5, i);

		query = Query.fromQL("name <= 'foxtrot' and name >= 'bravo'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 1;
		for (Entity entity : r.getEntities()) {
			assertEquals(alphabet[i], entity.getProperty("name"));
			i++;
		}
		assertEquals(6, i);

		query = Query
				.fromQL("name <= 'foxtrot' and name >= 'bravo' order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 6;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(1, i);

		query = Query
				.fromQL("name < 'foxtrot' and name > 'bravo' order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 5;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(2, i);

		query = Query
				.fromQL("name < 'foxtrot' and name >= 'bravo' order by name desc");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		i = 5;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(1, i);

		query = Query.fromQL("name = 'foxtrot'");
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());

		long created = r.getEntity().getCreated();
		UUID entityId = r.getEntity().getUuid();

		query = Query.fromQL("created = " + created);
		r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		assertEquals(1, r.size());
		assertEquals(entityId, r.getEntity().getUuid());

	}

	@Test
	public void testSecondarySorts() throws Exception {
		logger.info("testSecondarySorts");

		UUID applicationId = createApplication("testOrganization",
				"testSecondarySorts");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		for (int i = alphabet.length - 1; i >= 0; i--) {
			String name = alphabet[i];
			Map<String, Object> properties = new LinkedHashMap<String, Object>();
			properties.put("name", name);
			properties.put("group", i / 3);
			properties.put("reverse_name", alphabet[alphabet.length - 1 - i]);

			em.create("item", properties);

		}

		Query query = Query.fromQL("group = 1 order by name desc");
		Results r = em.searchCollection(em.getApplicationRef(), "items", query);
		logger.info(JsonUtils.mapToFormattedJsonString(r.getEntities()));
		int i = 6;
		for (Entity entity : r.getEntities()) {
			i--;
			assertEquals(1L, entity.getProperty("group"));
			assertEquals(alphabet[i], entity.getProperty("name"));
		}
		assertEquals(3, i);

	}
	

  @Test
  public void testPropertyUpdateWithConnection() throws Exception {

    UUID applicationId = createApplication("testOrganization", "testPropertyUpdateWithConnection");

    EntityManager em = emf.getEntityManager(applicationId);
    

    Map<String, Object> entity1 = new LinkedHashMap<String, Object>();
    entity1.put("name", "name_1");
    entity1.put("status", "pickled");    
    

    Map<String, Object> entity2 = new LinkedHashMap<String, Object>();
    entity2.put("name", "name_2");
    entity2.put("status", "foo"); 
    
    

    Entity entity1Ref =  em.create("names", entity1);
    Entity entity2Ref = em.create("names", entity2);
    
    
    em.createConnection(entity2Ref, "connecting", entity1Ref);
    
    //should return valid values
    Query query = Query.fromQL("select * where status = 'pickled'");

    Results r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());

    
    em.searchConnections(entity2Ref, query);
    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());
    
    //now update the first entity, this causes the failure after connections
    entity1Ref.setProperty("status", "herring");
    
    em.update(entity1Ref);
    
    //query and check the status has been updated, shouldn't return results
    query = Query.fromQL("select * where status = 'pickled'");

    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(0, r.size());
    
    //search connections
    em.searchConnections(entity2Ref, query);
    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(0, r.size());

    
    //should return results
    query = Query.fromQL("select * where status = 'herring'");

    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());
    
    
    //search connections
    em.searchConnections(entity2Ref, query);
    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());
  }
  
  /**
   * Same as above, but verifies the data in our entity_index_entry CF after the operations have completed
   * @throws Exception
   */
  
  @Test
  public void testPropertyUpdateWithConnectionEntityIndexEntryAudit() throws Exception {

    UUID applicationId = createApplication("testOrganization", "testPropertyUpdateWithConnectionEntityIndexEntryAudit");

    EntityManager em = emf.getEntityManager(applicationId);
    

    Map<String, Object> entity1 = new LinkedHashMap<String, Object>();
    entity1.put("name", "name_1");
    entity1.put("status", "pickled");    
    

    Map<String, Object> entity2 = new LinkedHashMap<String, Object>();
    entity2.put("name", "name_2");
    entity2.put("status", "foo"); 
    
    

    Entity entity1Ref =  em.create("names", entity1);
    Entity entity2Ref = em.create("names", entity2);
    
    
    em.createConnection(entity2Ref, "connecting", entity1Ref);
    
    //should return valid values
    Query query = Query.fromQL("select * where status = 'pickled'");

    Results r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());

    
    em.searchConnections(entity2Ref, query);
    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());
    
    //now update the first entity, this causes the failure after connections
    entity1Ref.setProperty("status", "herring");
    
    em.update(entity1Ref);
    
    //query and check the status has been updated, shouldn't return results
    query = Query.fromQL("select * where status = 'pickled'");

    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(0, r.size());
    
    //search connections
    em.searchConnections(entity2Ref, query);
    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(0, r.size());

    
    //should return results
    query = Query.fromQL("select * where status = 'herring'");

    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());
    
    
    //search connections
    em.searchConnections(entity2Ref, query);
    r = em.searchCollection(em.getApplicationRef(), "names", query);
    assertEquals(1, r.size());
    assertEquals(entity1Ref.getUuid(), r.getEntity().getUuid());
    
    
    RelationManagerImpl impl = (RelationManagerImpl) em.getRelationManager(entity2Ref);
    
    //now read the index and see what properties are there
    
    
    CassandraService cass = CassandraRunner.getBean(CassandraService.class);
    
    ByteBufferSerializer buf = ByteBufferSerializer.get();
    
    Keyspace ko = cass.getApplicationKeyspace(applicationId);
    Mutator<ByteBuffer> m = createMutator(ko, buf);

    
    IndexUpdate update = impl.batchStartIndexUpdate(m, entity1Ref, "status", "ignore", UUIDUtils.newTimeUUID(), false, false, true, false );
    
    int count = 0;
    
    IndexEntry lastMatch = null;
    
    for(IndexEntry entry: update.getPrevEntries()){
      if("status".equals(entry.getPath())){
        count++;
        lastMatch = entry;
      }
    }
    
    
    assertEquals(1, count);
    
    assertEquals("herring", lastMatch.getValue());
  }
  

}
