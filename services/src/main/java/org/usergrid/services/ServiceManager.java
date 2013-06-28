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
package org.usergrid.services;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.usergrid.batch.service.SchedulerService;
import org.usergrid.locking.LockManager;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.entities.Application;
import org.usergrid.services.ServiceParameter.IdParameter;
import org.usergrid.services.applications.ApplicationsService;
import org.usergrid.services.exceptions.UndefinedServiceEntityTypeException;
import org.usergrid.utils.ListUtils;

import static org.usergrid.persistence.SimpleEntityRef.ref;
import static org.usergrid.utils.InflectionUtils.pluralize;

public class ServiceManager {


    private static final Logger logger = LoggerFactory
            .getLogger(ServiceManager.class);
    
    /**
     * A pointer that signals we couldn't find a class
     */
    private static final Class<Service>  NOTFOUNDPOINTER = Service.class; 
    
	// because one typo can ruin your whole day
	public static final String ENTITY = "entity";
	public static final String ENTITY_SUFFIX = "." + ENTITY;
	public static final String COLLECTION = "collection";
	public static final String COLLECTION_SUFFIX = "." + COLLECTION;
	public static final String OSS_PACKAGE_PREFIX = "org.usergrid.services";
	public static final String COM_PACKAGE_PREFIX = "com.usergrid.services";
	public static final String SERVICE_PACKAGE_PREFIXES = "usergird.service.packages";

    public static final String APPLICATION_REQUESTS = "application.requests";
    public static final String APPLICATION_REQUESTS_PER = APPLICATION_REQUESTS + ".";
    public static final String IMPL = "Impl";

	private Application application;
	
	private UUID applicationId;

	private EntityManager em;

	private ServiceManagerFactory smf;

	// search for commercial packages first for SaaS version
	public static String[] package_prefixes = { COM_PACKAGE_PREFIX,
			OSS_PACKAGE_PREFIX };

	boolean searchPython;

	public ServiceManager() {
	}

	public ServiceManager init(ServiceManagerFactory smf, EntityManager em, Properties properties) {
		this.smf = smf;
		this.em = em;

		if (em != null) {
			try {
                application = em.getApplication();
                applicationId = em.getApplicationRef().getUuid();
            } catch (Exception e) {
                logger.error("This should never happen", e);
                throw new RuntimeException(e);
            }
		}
		if( properties != null ) {
			String packages = properties.getProperty(SERVICE_PACKAGE_PREFIXES);
			if( !StringUtils.isEmpty(packages) ) {
				setServicePackagePrefixes(packages);
			}
		}
		return this;
	}
	
	public ApplicationContext getApplicationContext() {
	    return smf.getApplicationContext();
	}
	
	private void setServicePackagePrefixes(String packages) {
		List<String> packagePrefixes = new ArrayList<String>();
		for(String prefix : package_prefixes)
			packagePrefixes.add(prefix);
		
		String[] prefixes = packages.split(";");
		for(String prefix : prefixes) {
			if( !packagePrefixes.contains(prefix) )
				packagePrefixes.add(prefix);
		}
		package_prefixes = packagePrefixes.toArray(new String[packagePrefixes.size()]);
	}

	public EntityManager getEntityManager() {
		return em;
	}

	public UUID getApplicationId() {
		return application.getUuid();
	}

	/**
	 * Return true if our current applicationId is the managment Id
	 * @return
	 */
	public boolean isMangementApplication(){
	    return CassandraService.MANAGEMENT_APPLICATION_ID.equals(getApplicationId());
	}
	
	public EntityRef getApplicationRef() {
		return ref(Application.ENTITY_TYPE, applicationId);
	}
	

	public Application getApplication(){
	    return application;
	}

	public Service getEntityService(String entityType) {
		String serviceType = "/" + pluralize(entityType);
		return getService(serviceType);
	}

	public Entity importEntity(ServiceRequest request, Entity entity)
			throws Exception {
		Service service = getEntityService(entity.getType());
		if (service != null) {
			return service.importEntity(request, entity);
		}
		return entity;
	}

	public Entity writeEntity(ServiceRequest request, Entity entity)
			throws Exception {
		Service service = getEntityService(entity.getType());
		if (service != null) {
			return service.writeEntity(request, entity);
		}
		return entity;
	}

	public Entity updateEntity(ServiceRequest request, EntityRef ref,
			ServicePayload payload) throws Exception {
		Service service = getEntityService(ref.getType());
		if (service != null) {
			return service.updateEntity(request, ref, payload);
		}
		return null;
	}

	public Service getService(String serviceType) {
		return getService(serviceType, true);
	}

	public Service getService(String serviceType, boolean fallback) {

		if (serviceType == null) {
			return null;
		}

		logger.debug("Looking up service pattern: {}", serviceType);

		ServiceInfo info = ServiceInfo.getServiceInfo(serviceType);

		if (info == null) {
			return null;
		}

		Service service = getServiceInstance(info);

		if (service != null) {
			logger.debug("Returning service instance: {}", service.getClass());
		}

		/*
		 * if ((service == null) && fallback) { for (String pattern :
		 * info.getPatterns()) { service = getService(pattern, false); if
		 * (service != null) { break; } } }
		 */

		if (service == null) {
			logger.info("Service {} not found", serviceType);
		}

		return service;
	}

    private static LoadingCache<ServiceInfo, Class<Service> > serviceClassCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<ServiceInfo, Class<Service> >() {
                        public Class<Service>  load(ServiceInfo key) { // no checked exception
                            return findClass(key);
                        }
                    });


    /**
     * For the service info find the class that this maps 
     * @param info
     * @return
     */
    private static Class<Service>  findClass(ServiceInfo info){
        Class<Service>  cls = null;
        
        String cname = null;
        
        for (String pattern : info.getPatterns()) {
            for (String prefix : package_prefixes) {
            
                cname = prefix.concat(".").concat(ServiceInfo.getClassName(pattern));
                
                cls = findClass(cname);
                
                if(cls != null){
                    return cls;
                }
              
               
            }
        }
        
        //We didn't find anything, return the not found pointer
        return NOTFOUNDPOINTER;
    }
    
    
    @SuppressWarnings("unchecked")
    private static Class<Service>  findClass(String classname) {
       
        Class<Service>  cls;
        try {
            logger.debug("Attempting to instantiate service class {}", classname);
            cls = (Class<Service>) Class.forName(classname);
            if (cls.isInterface()) {
                cls = (Class<Service>) Class.forName(classname.concat(IMPL));
            }
            if ((cls != null)
                    && !Modifier.isAbstract(cls.getModifiers())) {
                return cls;
            }
        } catch (ClassNotFoundException e1) {
            logger.debug("Could not load class", e1);
        }
        return null;
    }


	private Class<Service> findServiceClass(ServiceInfo info) {
		Class<Service> cls = null;
		
		try {
            cls = serviceClassCache.get(info);
        } catch (ExecutionException e) {
            //shouldn't happen, just to be safe
            throw new RuntimeException(e);
        }
		
		//Makes me feel dirty on the inside, but neccessary for the guava cache non null
		if(cls == NOTFOUNDPOINTER){
		    return null;
		}
		
                    
		return cls;
	}

	private Service getServiceInstance(ServiceInfo info) {

		Class<Service> cls = findServiceClass(info);
		if (cls != null) {
			Service s = null;
			try {
				try {
					//s = applicationContext.getAutowireCapableBeanFactory().createBean(cls);
				    s = cls.newInstance();
				} catch (Exception e) {
				}
//			TODO TN I don't think this is used anymore	
//				if (s == null) {
//					try {
//						String cname = cls.getName();
//                        cls = serviceClassCache.get(cname.concat(IMPL));
//						s = applicationContext.getAutowireCapableBeanFactory()
//								.createBean(cls);
//					} catch (Exception e) {
//					    //we can't find what we're looking for
//                        return null;
//					}
//				}
			} catch (Exception e) {
                e.printStackTrace();
			}
			if (s instanceof AbstractService) {
				AbstractService as = ((AbstractService) s);
				as.setServiceManager(this);
				as.init(info);
			}
			if (s != null) {
				if (s.getEntityType() == null) {
					throw new UndefinedServiceEntityTypeException();
				}
			}
			return s;
		}

		return null;
	}

	public ServiceRequest newRequest(ServiceAction action,
			List<ServiceParameter> parameters) throws Exception {
		return newRequest(action, false, parameters, null);
	}

	public ServiceRequest newRequest(ServiceAction action,
			List<ServiceParameter> parameters, ServicePayload payload)
			throws Exception {
		return newRequest(action, false, parameters, payload);
	}

	private ServiceRequest getApplicationRequest(ServiceAction action,
			boolean returnsTree, List<ServiceParameter> parameters,
			ServicePayload payload) throws Exception {

		String serviceName = pluralize(Application.ENTITY_TYPE);
		ListUtils.requeue(parameters, new IdParameter(applicationId));
		return new ServiceRequest(this, action, serviceName, parameters,
				payload, returnsTree);
	}

	static ApplicationsService appService = new ApplicationsService();

	public ServiceRequest newRequest(ServiceAction action, boolean returnsTree,
			List<ServiceParameter> parameters, ServicePayload payload)
			throws Exception {

		if (em != null) {
			if (action != null) {
				em.incrementAggregateCounters(null, null, null,
						APPLICATION_REQUESTS_PER.concat(action.toString().toLowerCase()), 1);
			}
		}

		if (!ServiceParameter.moreParameters(parameters)) {
			return getApplicationRequest(action, returnsTree, parameters,
					payload);
		}

		if (!ServiceParameter.firstParameterIsName(parameters)) {
			return null;
		}

		String nameParam = ServiceParameter.firstParameter(parameters)
				.getName();
		if (appService.hasEntityCommand(nameParam)
				|| appService.hasEntityDictionary(nameParam)) {
			return getApplicationRequest(action, returnsTree, parameters,
					payload);
		}

		String serviceName = pluralize(ServiceParameter.dequeueParameter(
				parameters).getName());
		return new ServiceRequest(this, action, serviceName, parameters,
				payload, returnsTree);
	}

	public void notifyExecutionEventListeners(ServiceAction action,
			ServiceRequest request, ServiceResults results,
			ServicePayload payload) {
		smf.notifyExecutionEventListeners(action, request, results, payload);
	}

	public void notifyCollectionEventListeners(String path,
			ServiceResults results) {
		smf.notifyCollectionEventListeners(path, results);
	}

  public SchedulerService getSchedulerService() {
    return smf.getSchedulerService();
  }

  public LockManager getLockManager() {
    return smf.getLockManager();
  }
}
