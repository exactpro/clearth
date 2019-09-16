/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
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
 ******************************************************************************/

package com.exactprosystems.clearth.automation.matrix.linked;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class GoogleSpreadsheetKeyLoader
{
	public InputStream getDecryptedFilePath(Path filePath) throws Exception
	{
		if (filePath == null || !Files.exists(filePath))
			throw new IllegalArgumentException("pkcs12 file not found");
		return new ByteArrayInputStream(Files.readAllBytes(filePath));
	}
}
