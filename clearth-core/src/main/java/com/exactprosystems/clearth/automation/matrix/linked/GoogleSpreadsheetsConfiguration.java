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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GoogleSpreadsheetsConfiguration {
	
	private final boolean enabled;
	
	private String serviceEmail;
	private Path p12FileName;
	
	private String keyStorePass;
	private String alias;
	private String keyPass;

	public GoogleSpreadsheetsConfiguration(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getServiceEmail() {
		return serviceEmail;
	}

	public void setServiceEmail(String serviceEmail) {
		this.serviceEmail = serviceEmail;
	}

	public Path getP12FileName() {
		return p12FileName;
	}

	public void setP12FileName(Path p12FileName) {
		this.p12FileName = p12FileName;
	}

	public String getKeyStorePass() {
		return keyStorePass;
	}

	public void setKeyStorePass(String keyStorePass) {
		this.keyStorePass = keyStorePass;
	}

	public String getAlias() {
		return alias;
	}

	public void setAllias(String allias) {
		this.alias = allias;
	}

	public String getKeyPass() {
		return keyPass;
	}

	public void setKeyPass(String keyPass) {
		this.keyPass = keyPass;
	}
	
	public static GoogleSpreadsheetsConfiguration disable() {
		return new GoogleSpreadsheetsConfiguration(false);
	}
	
	public String check() {
		if (!this.enabled)
			return null;
		List<String> missedValues = new ArrayList<>();
		StringBuilder errorText = new StringBuilder();
		
		this.checkField("Service Email", serviceEmail, missedValues);
		this.checkField("Key store password", keyStorePass, missedValues);
		this.checkField("Alias", alias, missedValues);
		this.checkField("Key pass", keyPass, missedValues);
		this.checkField("P12 Key File", p12FileName, missedValues);

		if (missedValues.size() > 0)
			errorText.append(missedValues.stream().collect(Collectors.joining(", ", "[", "]")))
				.append(" missed. ");
		
		if (p12FileName != null && !Files.exists(p12FileName))
			errorText.append("P12 Key File does not exist. ");
		
		if (errorText.length() > 0) {
			errorText.append("Please configure correctly.");
			return errorText.toString();
		} else {
			return null;
		}
		
	}
	
	private void checkField(String name, Object value, List<String> missedValues) {
		if (value == null) {
			missedValues.add(name);
		}
	}
}
