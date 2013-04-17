package org.usergrid.rest.applications.activities;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.app.ActivitiesCollection;
import org.usergrid.utils.MapUtils;
import org.usergrid.utils.UUIDUtils;

public class ActivityResourceTest extends RestContextTest {

  @Test
  public void activityPaging() {

    ActivitiesCollection activities = context.application().activities();

  
    final int pageSize = 15;
    final int pages = 3;
    final int count = pageSize*pages;
    
    for (int i = 0; i < count; i++) {
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("id", i);
      data.put("actor",  MapUtils.hashMap("displayName", "Bob").map("username", "bob"));
      data.put("verb", "POST");
      
      activities.post(data);
    }
    
    
    UUID start = UUIDUtils.MAX_TIME_UUID;

    activities = activities.withLimit(pageSize);

    int compareId = count-1;

    for (int i = 0; i < pages; i++) {

      activities = activities.withStart(start);

      List<JsonNode> entities = activities.getEntities();

      for (JsonNode node : entities) {
        assertEquals(compareId, node.get("id").asInt());

        start = UUID.fromString(node.get("uuid").asText());
        compareId--;
      }

    }

    activities = activities.withStart(start);

    List<JsonNode> entities = activities.getEntities();

    assertEquals(0, entities.size());

    // reversed ordering

    start = UUIDUtils.MIN_TIME_UUID;

    activities = activities.withReversed(true).withLimit(pageSize);

    compareId = 0;

    for (int i = 0; i < pages; i++) {

      activities = activities.withStart(start);

      entities = activities.getEntities();

      for (JsonNode node : entities) {
        assertEquals(compareId, node.get("id").asInt());

        start = UUID.fromString(node.get("uuid").asText());
        compareId++;
      }
    }

  }


}
