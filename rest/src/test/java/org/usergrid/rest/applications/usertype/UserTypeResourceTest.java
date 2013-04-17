package org.usergrid.rest.applications.usertype;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.RestContextTest;
import org.usergrid.rest.test.resource.app.UserTypeCollection;
import org.usergrid.utils.UUIDUtils;

public class UserTypeResourceTest extends RestContextTest {

  @Test
  public void uuidBasedPaging() {

    UserTypeCollection things = context.application().userType("things");

    final int pageSize = 15;
    final int pages = 3;
    final int count = pageSize * pages;

    for (int i = 0; i < count; i++) {
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("id", i);

      things.post(data);
    }

    // test reverse ordering

    UUID start = UUIDUtils.MIN_TIME_UUID;

    things = things.withLimit(pageSize).withReversed(false);

    int compareId = 0;

    for (int i = 0; i < pages; i++) {

      things = things.withStart(start);

      List<JsonNode> entities = things.getEntities();

      for (JsonNode node : entities) {
        assertEquals(compareId, node.get("id").asInt());

        start = UUID.fromString(node.get("uuid").asText());
        compareId++;
      }

    }

    things = things.withStart(start);

    List<JsonNode> entities = things.getEntities();

    assertEquals(0, entities.size());

    // reversed ordering

    start = UUIDUtils.MAX_TIME_UUID;

    things = things.withReversed(true).withLimit(pageSize);

    compareId = count - 1;

    for (int i = 0; i < pages; i++) {

      things = things.withStart(start);

      entities = things.getEntities();

      for (JsonNode node : entities) {
        assertEquals(compareId, node.get("id").asInt());

        start = UUID.fromString(node.get("uuid").asText());
        compareId--;
      }
    }

  }

}
