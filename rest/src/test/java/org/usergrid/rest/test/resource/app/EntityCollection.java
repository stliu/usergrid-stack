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
package org.usergrid.rest.test.resource.app;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.usergrid.rest.test.resource.NamedResource;
import org.usergrid.rest.test.resource.SetResource;

import com.sun.jersey.api.client.WebResource;

/**
 * @author tnine
 * 
 */
public abstract class EntityCollection extends SetResource {

 
  private UUID start;
  private int limit;
  private boolean reversed;
  

  /**
   * @param collectionName
   * @param parent
   */
  public EntityCollection(String collectionName, NamedResource parent) {
    super(collectionName, parent);
  }
  
  @SuppressWarnings("unchecked")
  public <T extends EntityCollection> T withStart(UUID start){
    this.start = start;
    return (T)this;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends EntityCollection> T withLimit(int limit){
    this.limit = limit;
    return (T)this;
  }
  
  public <T extends EntityCollection> T withReversed(boolean reversed){
    this.reversed = reversed;
    return (T)this;
  }


  
  /**
   * Get entities in this collection. Cursor is optional
   * 
   * @param query
   * @param cursor
   * @return
   */
  public JsonNode get() {
    return getInternal();
  }
  
  /**
   * Get entities
   * @return
   */
  public List<JsonNode> getEntities(){
    return getNodesAsList("entities", get());
  }
  
  /**
   * Post an entity to this collection
   * @param payload
   * @return
   */
  public JsonNode post(Map<String, ?> payload) {
    return postInternal(payload);
  }

  /**
   * post to the entity set
   * 
   * @param entity
   * @return
   */
  public JsonNode delete() {
    return jsonMedia(withToken(resource())).delete(JsonNode.class);
  }

  /**
   * Set the queue client ID if set
   * 
   * @param resource
   * @return
   */
  protected WebResource withParams(WebResource resource) {
    if (start != null) {
      resource = resource.queryParam("start", start.toString());
    }
    
    if (limit > 0) {
      resource = resource.queryParam("limit", String.valueOf(limit));
    }
    
    if(reversed){
      resource = resource.queryParam("reversed", "true"); 
    }
   

    return resource;
  }
  
 
}
