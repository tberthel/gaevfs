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
package com.newatlanta.appengine.nio.file;

import java.io.IOException;

import com.newatlanta.repackaged.java.nio.file.FileStore;
import com.newatlanta.repackaged.java.nio.file.attribute.FileAttributeView;
import com.newatlanta.repackaged.java.nio.file.attribute.FileStoreAttributeView;

public class GaeFileStore extends FileStore {
    
    private static GaeFileStore instance = new GaeFileStore();
    
    public static GaeFileStore getInstance() {
        return instance;
    }

    private GaeFileStore() {
    }

    @Override
    public Object getAttribute( String attribute ) throws IOException {
        return null;
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView( Class<V> type ) {
        return null; // TODO: default block size?
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String name() {
        return "GaeVFS";
    }

    @Override
    public boolean supportsFileAttributeView( Class<? extends FileAttributeView> type ) {
        return false; // TODO: default block size?
    }

    @Override
    public boolean supportsFileAttributeView( String name ) {
        return false; // TODO: default block size?
    }

    @Override
    public String type() {
        return "";
    }

}
