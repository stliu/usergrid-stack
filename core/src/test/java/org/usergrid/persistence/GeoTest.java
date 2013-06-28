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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.beoui.geocell.model.Point;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.cassandra.GeoIndexManager;
import org.usergrid.persistence.cassandra.GeoIndexManager.EntityLocationRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.usergrid.persistence.Results.Level.ALL_PROPERTIES;

public class GeoTest extends AbstractPersistenceTest {

	private static final Logger logger = LoggerFactory.getLogger(GeoTest.class);

	public GeoTest() {
		super();
	}

	@Test
	public void testGeo() throws Exception {
		logger.info("GeoTest.testGeo");

		UUID applicationId = createApplication("testOrganization","testGeo");
		assertNotNull(applicationId);

		EntityManager em = emf.getEntityManager(applicationId);
		assertNotNull(em);

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("username", "edanuff");
		properties.put("email", "ed@anuff.com");

		Entity user = em.create("user", properties);
		assertNotNull(user);

		EntityLocationRef loc = new EntityLocationRef(user, 37.776753,
				-122.407846);
		GeoIndexManager geo = em.getGeoIndexManager();
		geo.storeLocationInCollectionIndex(em.getApplicationRef(), "users", user.getUuid(),
				"location.coordinates", loc);

		Point center = new Point(37.774277, -122.404744);
		Results results = geo.proximitySearchCollection(em.getApplicationRef(),
				"users", "location.coordinates", center, 200, null, 10, false,
				ALL_PROPERTIES);

		assertEquals(0, results.size());

		results = geo.proximitySearchCollection(em.getApplicationRef(),
				"users", "location.coordinates", center, 400, null, 10, false,
				ALL_PROPERTIES);

		this.dump(results.getEntities());

		assertEquals(1, results.size());

		geo.removeLocationFromCollectionIndex(em.getApplicationRef(), "users",
				"location.coordinates", loc);

		results = geo.proximitySearchCollection(em.getApplicationRef(),
				"users", "location.coordinates", center, 400, null, 10, false,
				ALL_PROPERTIES);

		this.dump(results.getEntities());

		assertEquals(0, results.size());

		updatePos(em, user, 37.426373, -122.14108);

		center = new Point(37.774277, -122.404744);
		results = geo.proximitySearchCollection(em.getApplicationRef(),
				"users", "location.coordinates", center, 200, null, 10, false,
				ALL_PROPERTIES);

		assertEquals(0, results.size());

		updatePos(em, user, 37.774277, -122.404744);

		center = new Point(37.776753, -122.407846);
		results = geo.proximitySearchCollection(em.getApplicationRef(),
				"users", "location.coordinates", center, 1000, null, 10, false,
				ALL_PROPERTIES);

		assertEquals(1, results.size());

    // check at globally large distance
    results = geo.proximitySearchCollection(em.getApplicationRef(),
        "users", "location.coordinates", center, Integer.MAX_VALUE, null, 10, false, ALL_PROPERTIES);
    assertEquals(1, results.size());

    // create a new entity so we have 2
    LinkedHashMap<String, Object> properties2 = new LinkedHashMap<String, Object>();
    properties2.put("username", "sganyo");
    properties2.put("email", "sganyo@anuff.com");
    Entity user2 = em.create("user", properties2);
    assertNotNull(user2);
    EntityLocationRef loc2 = new EntityLocationRef(user2, 31.1, 121.2);
    geo.storeLocationInCollectionIndex(em.getApplicationRef(), "users", user2.getUuid(), "location.coordinates", loc2);

    // check at 10000m distance
    results = geo.proximitySearchCollection(em.getApplicationRef(),
        "users", "location.coordinates", center, 10000, null, 10, false, ALL_PROPERTIES);
    assertEquals(1, results.size());

    // check at globally large distance
    results = geo.proximitySearchCollection(em.getApplicationRef(),
        "users", "location.coordinates", center, Integer.MAX_VALUE, null, 10, false, ALL_PROPERTIES);
    assertEquals(2, results.size());

    // check at globally large distance (center point close to other entity)
    center = new Point(31.14, 121.27);
    results = geo.proximitySearchCollection(em.getApplicationRef(),
        "users", "location.coordinates", center, Integer.MAX_VALUE, null, 10, false, ALL_PROPERTIES);
    assertEquals(2, results.size());

    results = em.searchCollection(em.getApplicationRef(), "users",
				Query.fromQL("location within 1000 of 37.776753, -122.407846"));
		assertEquals(1, results.size());

		updatePos(em, user, 37.776753, -122.407846);

		center = new Point(37.428526, -122.140916);
		results = geo.proximitySearchCollection(em.getApplicationRef(),
				"users", "location.coordinates", center, 1000, null, 10, false,
				ALL_PROPERTIES);

		assertEquals(0, results.size());

		results = em.searchCollection(em.getApplicationRef(), "users",
				Query.fromQL("location within 1000 of 37.428526, -122.140916"));
		assertEquals(0, results.size());

		properties = new LinkedHashMap<String, Object>();
		properties.put("name", "Brickhouse");
		properties.put("address", "426 Brannan Street");
		properties.put("location", getLocation(37.779632, -122.395131));

		Entity restaurant = em.create("restaurant", properties);
		assertNotNull(restaurant);

		em.createConnection(user, "likes", restaurant);

		results = em.searchConnectedEntities(user,
				Query.fromQL("location within 2000 of 37.776753, -122.407846"));
		assertEquals(1, results.size());

		results = em.searchConnectedEntities(user,
				Query.fromQL("location within 1000 of 37.776753, -122.407846"));
		assertEquals(0, results.size());

	}

	public Map<String, Object> getLocation(double latitude, double longitude)
			throws Exception {
		Map<String, Object> latlong = new LinkedHashMap<String, Object>();
		latlong.put("latitude", latitude);
		latlong.put("longitude", longitude);
		return latlong;
	}

	public void updatePos(EntityManager em, EntityRef entity, double latitude,
			double longitude) throws Exception {
		Map<String, Object> latlong = new LinkedHashMap<String, Object>();
		latlong.put("latitude", latitude);
		latlong.put("longitude", longitude);

		em.setProperty(entity, "location", latlong);
	}
}
