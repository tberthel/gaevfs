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

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.local.GenericFileNameParser;

/**
 * Creates GaeFileName instances.
 *  
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 * @version $Revision: 1.2 $ $Date: 2009/06/23 20:51:24 $
 */
public class GaeFileNameParser extends GenericFileNameParser {
	
	private static GaeFileNameParser instance = new GaeFileNameParser();
	
	public static GaeFileNameParser getInstance() {
        return instance;
    }

	@Override
	protected FileName createFileName( String scheme, String rootFile, String path, FileType type ) {
		return new GaeFileName( scheme, path, type );
	}
}
