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
 * A utility class that wraps an Iterable<Entity> and treats it as an
 * Iterable<Key>; this is useful when doing keys-only queries to treat
 * the return value as an Iterable<Key>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class EntityKeyCollection extends AbstractCollection<Key> {
    
    private Iterable<Entity> entityIterable;
   
    public static EntityKeyCollection wrap( Iterable<Entity> entityIterable ) {
        return new EntityKeyCollection( entityIterable );
    }
    
    private EntityKeyCollection( Iterable<Entity> entityIterable ) {
        this.entityIterable = entityIterable;
    }

    @Override
    public Iterator<Key> iterator() {
        return new EntityKeyIterator( entityIterable.iterator() );
    }

    @Override
    public int size() {
        if ( entityIterable instanceof Collection<?> ) {
            return ((Collection<?>)entityIterable).size();
        }
        throw new UnsupportedOperationException();
    }
    
    private class EntityKeyIterator implements Iterator<Key> {
        
        private Iterator<Entity> entityIterator;
        
        private EntityKeyIterator( Iterator<Entity> entityIterator ) {
            this.entityIterator = entityIterator;
        }

        @Override
        public boolean hasNext() {
            return entityIterator.hasNext();
        }

        @Override
        public Key next() {
             Entity entity = entityIterator.next();
             return entity.getKey();
        }

        @Override
        public void remove() {
            entityIterator.remove();
        }
    }
}
