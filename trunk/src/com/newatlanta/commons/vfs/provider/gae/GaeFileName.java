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

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileName;

/**
 * Supports serialization required by <code>memcache</code> (see
 * {@link com.newatlanta.commons.vfs.cache.GaeMemcacheFilesCache}). This is an
 * internal GaeVFS implementation class that is
 * normally not referenced directly, but only indirectly via the
 * <a href="http://commons.apache.org/vfs/apidocs/index.html" target="_blank">Apache
 * Commons VFS API</a>. See {@link GaeVFS} as the entry point for application
 * code that interacts with GaeVFS.
 *
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class GaeFileName extends AbstractFileName {

    private static final long serialVersionUID = 1L;

    GaeFileName( String scheme, String absPath, FileType type ) {
        super( scheme, absPath, type );
    }

    @Override
    protected void appendRootUri( StringBuffer buffer, boolean addPassword ) {
        buffer.append( getScheme() ).append( "://" );
    }

    @Override
    public FileName createName( String absPath, FileType type ) {
        return new GaeFileName( getScheme(), absPath, type );
    }
}
