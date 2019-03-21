/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.rest.util.ArrayConverter;
import org.springframework.data.neo4j.rest.util.ArrayConverter;

import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class RestEntity implements PropertyContainer {
    private Map<?, ?> structuralData;
    private Map<String, Object> propertyData;
    private long lastTimeFetchedPropertyData;
    private RestGraphDatabase graphDatabase;
    protected RestRequest restRequest;
    private final ArrayConverter arrayConverter=new ArrayConverter();

    public RestEntity( URI uri, RestGraphDatabase graphDatabase ) {
        this( uri.toString(), graphDatabase );
    }

    public RestEntity( String uri, RestGraphDatabase graphDatabase ) {
        this.restRequest = graphDatabase.getRestRequest().with( uri );
        this.graphDatabase = graphDatabase;
    }

    public RestEntity( Map<?, ?> data, RestGraphDatabase graphDatabase ) {
        this.structuralData = data;
        this.graphDatabase = graphDatabase;
        this.propertyData = (Map<String, Object>) data.get( "data" );
        this.lastTimeFetchedPropertyData = System.currentTimeMillis();
        String uri = (String) data.get( "self" );
        this.restRequest = graphDatabase.getRestRequest().with( uri );
    }

    public String getUri() {
        return this.restRequest.getUri().toString();
    }

    Map<?, ?> getStructuralData() {
        if ( this.structuralData == null ) {
            this.structuralData = restRequest.toMap( restRequest.get( "" ) );
        }
        return this.structuralData;
    }

    Map<String, Object> getPropertyData() {
        if ( this.propertyData == null || timeElapsed( this.lastTimeFetchedPropertyData, graphDatabase.getPropertyRefetchTimeInMillis() ) ) {
            RequestResult requestResult = restRequest.get( "properties" );
            boolean ok = restRequest.statusIs(requestResult, Status.OK );
            if ( ok ) {
                this.propertyData = (Map<String, Object>) restRequest.toMap(requestResult);
            } else {
                this.propertyData = Collections.emptyMap();
            }
            this.lastTimeFetchedPropertyData = System.currentTimeMillis();
        }
        return this.propertyData;
    }

    private boolean timeElapsed( long since, long isItGreaterThanThis ) {
        return System.currentTimeMillis() - since > isItGreaterThanThis;
    }

    public Object getProperty( String key ) {
        Object value = getPropertyValue( key );
        if ( value == null ) {
            throw new NotFoundException( "'" + key + "' on " + this );
        }
        return value;
    }

    private Object getPropertyValue( String key ) {
        Map<String, Object> properties = getPropertyData();
        Object value = properties.get( key );
        if ( value == null) return null;
        if ( value instanceof Collection ) {
            Collection col= (Collection) value;
            if (col.isEmpty()) return new String[0]; // todo concrete value type ?
            Object result = arrayConverter.toArray( col );
            if (result == null) throw new IllegalStateException( "Could not determine type of property "+key );
            properties.put(key,result);
            return result;

        }
        return PropertiesMap.assertSupportedPropertyValue( value );
    }

    public Object getProperty( String key, Object defaultValue ) {
        Object value = getPropertyValue( key );
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public Iterable<String> getPropertyKeys() {
        return new IterableWrapper( getPropertyData().keySet() ) {
            @Override
            protected String underlyingObjectToObject( Object key ) {
                return key.toString();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Iterable<Object> getPropertyValues() {
        return (Iterable<Object>) getPropertyData().values();
    }

    public boolean hasProperty( String key ) {
        return getPropertyData().containsKey( key );
    }

    public Object removeProperty( String key ) {
        Object value = getProperty( key, null );
        restRequest.delete( "properties/" + key );
        invalidatePropertyData();
        return value;
    }

    public void setProperty( String key, Object value ) {
        restRequest.put( "properties/" + key, JsonHelper.createJsonFrom( value ) );
        invalidatePropertyData();
    }

    private void invalidatePropertyData() {
        this.propertyData = null;
    }

    static long getEntityId( String uri ) {
        return Long.parseLong( uri.substring( uri.lastIndexOf( '/' ) + 1 ) );
    }

    public long getId() {
        return getEntityId( getUri() );
    }

    public void delete() {
        restRequest.delete( "" );
    }

    @Override
    public int hashCode() {
        return (int) getId();
    }

    @Override
    public boolean equals( Object o ) {
        if (o == null) return false;
        if (o == this) return true;
        return getClass().equals( o.getClass() ) && getId() == ( (RestEntity) o ).getId();
    }

    public GraphDatabaseService getGraphDatabase() {
        throw new UnsupportedOperationException("No GraphDatabaseService semantics for the REST-API");
    }

    public RestGraphDatabase getRestGraphDatabase() {
        return graphDatabase;
    }

    public RestRequest getRestRequest() {
        return restRequest;
    }

    @Override
    public String toString() {
        return getUri();
    }
}
