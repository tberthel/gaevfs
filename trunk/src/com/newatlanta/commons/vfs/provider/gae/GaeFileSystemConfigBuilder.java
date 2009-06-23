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

import org.apache.commons.vfs.FileSystemConfigBuilder;
import org.apache.commons.vfs.FileSystemOptions;

public class GaeFileSystemConfigBuilder extends FileSystemConfigBuilder {

	private static final String COMBINED_LOCAL_KEY = "combinedLocal";
	
	private static GaeFileSystemConfigBuilder instance = new GaeFileSystemConfigBuilder();
	
	public static GaeFileSystemConfigBuilder getInstance() {
		return instance;
	}
	
	@Override
	protected Class<GaeFileSystem> getConfigClass() {
		return GaeFileSystem.class;
	}

	public boolean getCombinedLocal( FileSystemOptions opts ) {
		Boolean combinedLocal = (Boolean)super.getParam( opts, COMBINED_LOCAL_KEY );
		return ( combinedLocal != null ? combinedLocal.booleanValue() : false );
	}
	
	public void setCombinedLocal( FileSystemOptions opts, boolean combinedLocal ) {
		super.setParam( opts, COMBINED_LOCAL_KEY, new Boolean( combinedLocal ) );
	}
}
