/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.commons.vfs.provider.gae;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.AbstractOriginatingFileProvider;

/**
 * Configures the GaeFileNameParser and creates GaeFileSystem instances.
 *
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 * @version $Revision: 1.2 $ $Date: 2009/06/23 20:51:24 $
 */
public class GaeFileProvider extends AbstractOriginatingFileProvider {
	
	public final static Collection<Capability> capabilities = Collections.unmodifiableCollection( Arrays.asList(
			Capability.CREATE,
			Capability.DELETE,
			Capability.GET_LAST_MODIFIED,
			Capability.GET_TYPE,
			Capability.LAST_MODIFIED,
			Capability.LIST_CHILDREN,
			Capability.READ_CONTENT,
			Capability.RENAME,
			Capability.SET_LAST_MODIFIED_FILE,
			Capability.URI,
			Capability.WRITE_CONTENT
			) );
	
	public GaeFileProvider() {
		setFileNameParser( GaeFileNameParser.getInstance() );
	}
	
	protected FileSystem doCreateFileSystem( FileName rootName, FileSystemOptions options ) throws FileSystemException {
		return new GaeFileSystem( rootName, options );
	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
