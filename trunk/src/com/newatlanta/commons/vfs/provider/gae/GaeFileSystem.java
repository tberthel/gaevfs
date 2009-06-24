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
package com.newatlanta.commons.vfs.provider.gae;

import java.util.Collection;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.AbstractFileSystem;

/**
 * Creates GaeFileObject instances.
 *
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeFileSystem extends AbstractFileSystem {

    protected GaeFileSystem( FileName rootName, FileSystemOptions fileSystemOptions ) {
        super( rootName, null, fileSystemOptions );
    }

    @Override
    public void init() throws FileSystemException {
        super.init();

        // make sure the root folder exists (why?!)
        if ( !getRoot().exists() ) {
            getRoot().createFolder();
        }
    }

    @SuppressWarnings("unchecked")
    protected void addCapabilities( Collection capabilities ) {
        capabilities.addAll( GaeFileProvider.capabilities );
    }

    protected FileObject createFile( FileName fileName ) {
        return new GaeFileObject( fileName, this );
    }
}