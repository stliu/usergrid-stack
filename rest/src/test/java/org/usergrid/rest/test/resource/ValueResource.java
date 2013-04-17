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
package org.usergrid.rest.test.resource;

import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

/**
 * @author tnine
 * 
 */
public abstract class ValueResource extends NamedResource {

  private static final Logger logger = LoggerFactory.getLogger(ValueResource.class);

  private String name;

  public ValueResource(String name, NamedResource parent) {
    super(parent);
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuilder)
   */
  @Override
  public void addToUrl(StringBuilder buffer) {
    parent.addToUrl(buffer);

    buffer.append(SLASH);

    buffer.append(name);
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  protected JsonNode postInternal(Map<String, ?> entity) {

    WebResource resource = withParams(withToken(resource()));

    logger.info("POST to {} with payload {}", resource.toString(), entity);

    return jsonMedia(resource).post(JsonNode.class, entity);
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  protected JsonNode postInternal(Map<String, ?>[] entity) {

    WebResource resource = withParams(withToken(resource()));

    logger.info("POST to {} with payload {}", resource.toString(), entity);

    return jsonMedia(resource).post(JsonNode.class, entity);
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  protected JsonNode putInternal(Map<String, ?> entity) {

    WebResource resource = withParams(withToken(resource()));

    logger.info("PUT to {}", resource.toString());

    return jsonMedia(resource).put(JsonNode.class, entity);
  }

  /**
   * Get a list of entities
   * 
   * @return
   */
  protected JsonNode getInternal() {
    WebResource resource = withParams(withToken(resource()));

    logger.info("GET from {}", resource.toString());

    return jsonMedia(resource).get(JsonNode.class);
  }

  /**
   * Get entities in this collection. Cursor is optional
   * 
   * @param query
   * @param cursor
   * @return
   */
  protected JsonNode getInternal(String query, String cursor) {

    WebResource resource = withParams(withToken(resource())).queryParam("ql", query);

    if (cursor != null) {
      resource = resource.queryParam("cursor", cursor);
    }

    logger.info("GET from {}", resource);

    return jsonMedia(resource).get(JsonNode.class);
  }

}
