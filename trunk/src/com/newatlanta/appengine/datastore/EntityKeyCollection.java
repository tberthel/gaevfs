/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.appengine.datastore;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

/**
 * A utility class that wraps an Collection<Entity> and treats it as an
 * Collection<Key>; this is useful when doing keys-only queries to treat
 * the return value as an Collection<Key>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class EntityKeyCollection extends AbstractCollection<Key> {
    
    private Collection<Entity> entities;
   
    public static EntityKeyCollection wrap( Collection<Entity> entities ) {
        return new EntityKeyCollection( entities );
    }
    
    private EntityKeyCollection( Collection<Entity> entityCollection ) {
        this.entities = entityCollection;
    }

    @Override
    public Iterator<Key> iterator() {
        return new EntityKeyIterator( entities.iterator() );
    }

    @Override
    public int size() {
        return entities.size();
    }
    
    private class EntityKeyIterator implements Iterator<Key> {
        
        private Iterator<Entity> entityIter;
        
        private EntityKeyIterator( Iterator<Entity> entityIter ) {
            this.entityIter = entityIter;
        }

        @Override
        public boolean hasNext() {
            return entityIter.hasNext();
        }

        @Override
        public Key next() {
             return entityIter.next().getKey();
        }

        @Override
        public void remove() {
            entityIter.remove();
        }
    }
}
