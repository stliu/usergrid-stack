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
package org.usergrid.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

/**
 * @author edanuff
 * 
 */
public class JsonUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(JsonUtils.class);

	static ObjectMapper mapper = new ObjectMapper();

	static SmileFactory smile = new SmileFactory();

    static ObjectMapper smileMapper = new ObjectMapper(smile);

    private static ObjectMapper indentObjectMapper = new ObjectMapper();

    static {
        indentObjectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

	/**
	 * @param obj
	 * @return
	 */
	public static String mapToJsonString(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		return "{}";

	}

	public static String mapToFormattedJsonString(Object obj) {
		try {
			return indentObjectMapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		return "{}";

	}

	public static String schemaToFormattedJsonString(JsonSchema schema) {
		return mapToFormattedJsonString(schema.getSchemaNode());
	}

	public static JsonSchema getJsonSchema(Class<?> cls) {
		JsonSchema jsonSchema = null;
		try {
			jsonSchema = mapper.generateJsonSchema(cls);
		} catch (JsonMappingException e) {
		}
		return jsonSchema;
	}

	public static Object parse(String json) {
		try {
			return mapper.readValue(json, Object.class);
		} catch (JsonParseException e) {
		} catch (JsonMappingException e) {
		} catch (IOException e) {
		}
		return null;
	}

    public static JsonNode parseToNode(String json) {
        try {
            return mapper.readValue(json, JsonNode.class);
        } catch (JsonParseException e) {
            logger.error("Unable to parse:", e);
        } catch (JsonMappingException e) {
            logger.error("Unable to parse:", e);
        } catch (IOException e) {
            logger.error("Unable to parse:", e);
        }
        return null;
    }

	public static JsonNode getJsonSchemaNode(Class<?> cls) {
		JsonNode schemaRootNode = null;
		JsonSchema jsonSchema = getJsonSchema(cls);
		if (jsonSchema != null) {
			schemaRootNode = jsonSchema.getSchemaNode();
		}
		return schemaRootNode;
	}

	public static String quoteString(String s) {
		JsonStringEncoder encoder = new JsonStringEncoder();
		return new String(encoder.quoteAsUTF8(s));
	}

	public static ByteBuffer toByteBuffer(Object obj) {
		if (obj == null) {
			return null;
		}

		byte[] bytes = null;
		try {
			bytes = smileMapper.writeValueAsBytes(obj);
		} catch (Exception e) {
			logger.error("Error getting SMILE bytes", e);
		}
		if (bytes != null) {
			return ByteBuffer.wrap(bytes);
		}
		return null;
	}

	public static Object fromByteBuffer(ByteBuffer byteBuffer) {
		return fromByteBuffer(byteBuffer, Object.class);
	}

	public static Object fromByteBuffer(ByteBuffer byteBuffer, Class<?> clazz) {
		if ((byteBuffer == null) || !byteBuffer.hasRemaining()) {
			return null;
		}
		if (clazz == null) {
			clazz = Object.class;
		}

		Object obj = null;
		try {
			obj = smileMapper.readValue(byteBuffer.array(), byteBuffer.arrayOffset()
					+ byteBuffer.position(), byteBuffer.remaining(), clazz);
		} catch (Exception e) {
			logger.error("Error parsing SMILE bytes", e);
		}
		return obj;
	}

	public static JsonNode nodeFromByteBuffer(ByteBuffer byteBuffer) {
		if ((byteBuffer == null) || !byteBuffer.hasRemaining()) {
			return null;
		}

		JsonNode obj = null;
		try {
			obj = smileMapper.readValue(byteBuffer.array(), byteBuffer.arrayOffset()
					+ byteBuffer.position(), byteBuffer.remaining(),
					JsonNode.class);
		} catch (Exception e) {
			logger.error("Error parsing SMILE bytes", e);
		}
		return obj;
	}

	public static JsonNode toJsonNode(Object obj) {
		if (obj == null) {
			return null;
		}
		JsonNode node = mapper.convertValue(obj, JsonNode.class);
		return node;
	}

	public static Map<String, Object> toJsonMap(Object obj) {
		if (obj == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = mapper.convertValue(obj, Map.class);
		return map;
	}

	private static UUID tryConvertToUUID(Object o) {
		if (o instanceof String) {
			String s = (String) o;
			if (s.length() == 36) {
				try {
					UUID uuid = UUID.fromString(s);
					return uuid;
				} catch (IllegalArgumentException e) {
				}
			}
		}
		return null;
	}
	
  public static Object normalizeJsonTree(Object obj) {
    return normalizeJsonTree(obj, false);
  }

	public static Object normalizeJsonTree(Object obj, boolean preserve_json_nodes) {
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> m = (Map<Object, Object>) obj;
            Object o;
            UUID uuid;
			for (Object k : m.keySet()) {
				o = m.get(k);
				uuid = tryConvertToUUID(o);
				if (uuid != null) {
					m.put(k, uuid);
				} else if (o instanceof Integer) {
					m.put(k, ((Integer) o).longValue());
				} else if (o instanceof BigInteger) {
					m.put(k, ((BigInteger) o).longValue());
				}
			}
		} else if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> l = (List<Object>) obj;
            Object o;
            UUID uuid;
			for (int i = 0; i < l.size(); i++) {
				o = l.get(i);
				uuid = tryConvertToUUID(o);
				if (uuid != null) {
					l.set(i, uuid);
				} else if ((o instanceof Map) || (o instanceof List)) {
					l.set(i, normalizeJsonTree(o, preserve_json_nodes));
				} else if (o instanceof Integer) {
					l.set(i, ((Integer) o).longValue());
				} else if (o instanceof BigInteger) {
					l.set(i, ((BigInteger) o).longValue());
				}
			}
		} else if (obj instanceof String) {
			UUID uuid = tryConvertToUUID(obj);
			if (uuid != null) {
				return uuid;
			}
		} else if (obj instanceof Integer) {
			return ((Integer) obj).longValue();
		} else if (obj instanceof BigInteger) {
			return ((BigInteger) obj).longValue();
		} else if (obj instanceof JsonNode) {
			Object o = mapper.convertValue(obj, Object.class);
			o = normalizeJsonTree(o, preserve_json_nodes);
			if (preserve_json_nodes) {
			  obj = mapper.convertValue(obj, JsonNode.class);
			}
			else {
			  obj = o;
			}
		}
		return obj;
	}

	public static Object select(Object obj, String path) {
		return select(obj, path, false);
	}

	public static Object select(Object obj, String path, boolean buildResultTree) {

		if (obj == null) {
			return null;
		}

		if (org.apache.commons.lang.StringUtils.isBlank(path)) {
			return obj;
		}

		String segment = stringOrSubstringBeforeFirst(path, '.');
		String remaining = substringAfter(path, ".");

		if (obj instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) obj;
			Object child = map.get(segment);
			Object result = select(child, remaining, buildResultTree);
			if (result != null) {
				if (buildResultTree) {
					Map<Object, Object> results = new LinkedHashMap<Object, Object>();
					results.put(segment, result);
					return results;
				} else {
					return result;
				}
			}
			return null;
		}
		if (obj instanceof List) {
			List<Object> results = new ArrayList<Object>();
			List<?> list = (List<?>) obj;
			for (Object i : list) {
				Object result = select(i, path, buildResultTree);
				if (result != null) {
					results.add(result);
				}
			}
			if (!results.isEmpty()) {
				return results;
			}
			return null;
		}

    if (obj instanceof Entity) {
      Object child = ((Entity)obj).getProperty(segment);
      Object result = select(child, remaining, buildResultTree);
      if (result != null) {
        if (buildResultTree) {
          Map<Object, Object> results = new LinkedHashMap<Object, Object>();
          results.put(segment, result);
          return results;
        } else {
          return result;
        }
      }
      else {
          return result;
      }
    }

		return obj;
	}

	public static Object loadJsonFromResourceFile(String file) {
		return loadJsonFromResourceFile(JsonUtils.class, Object.class, file);
	}
	
  public static <C> C loadJsonFromResourceFile(Class<?> resource_class, Class<C> target_class, String file) {
    C json = null;
    try {
      URL resource = resource_class.getResource(file);
      json = mapper.readValue(resource, target_class);
    } catch (Exception e) {
      logger.error("Error loading JSON", e);
    }
    return json;
  }
  

	public static Object loadJsonFromFilesystem(String filename) {
		Object json = null;
		try {
			File file = new File(filename);
			json = mapper.readValue(file, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static Object loadJsonFromUrl(String urlStr) {
		Object json = null;
		try {
			URL url = new URL(urlStr);
			json = mapper.readValue(url, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static Object loadJsonFromUrl(URL url) {
		Object json = null;
		try {
			json = mapper.readValue(url, Object.class);
		} catch (Exception e) {
			logger.error("Error loading JSON", e);
		}
		return json;
	}

	public static boolean isSmile(ByteBuffer buffer) {
		buffer = buffer.duplicate();
		if (buffer.get() != 0x3A) {
			return false;
		}
		if (buffer.get() != 0x29) {
			return false;
		}
		if (buffer.get() != 0x0A) {
			return false;
		}
		return true;
	}
}
