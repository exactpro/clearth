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

package com.exactprosystems.clearth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

/**
 * @author andrey.panarin
 *
 */
public class Digester
{
	/**
	 * Converts text data to an MD5 hash.
	 * @param data Input data to convert.
	 * @return The resulting MD5 hash, encoded as Base64. 
	 * @throws NoSuchAlgorithmException If MD5 is not supported.
	 * @throws UnsupportedEncodingException If UTF-8 is not supported.
	 */
	public static String stringToMD5(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] digested = md.digest(data.getBytes("UTF-8"));
		String base64 = Base64.encodeBase64String(digested).trim();
		return base64;
	}
	
	/**
	 * Checks if the specified string looks like an MD5 hash.
	 * @param data The string to check.
	 * @return true if the length and the ending of the string looks
	 * like the ones of an MD5/Base64 hash. Otherwise, returns false.
	 */
	public static boolean isLikeMD5(String data)
	{
		return data.length() == 24 && data.endsWith("==");
	}
	
	
	/**
	 * Run this digester once when you want to encrypt (digest) your 
	 * users' passwords.
	 * @param args Pass your salt here. If nothing is passed, 
	 * a default salt will be applied.
	 * @throws IOException In case of any I/O error reading or writing the users file.
	 * @throws NoSuchAlgorithmException If digestion algorithm is not available.
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException
	{
		String salt = ClearThCore.getDefaultSalt();
		
		if (args.length > 0)
		{
			System.out.println("Setting custom salt...");
			salt = args[0];
		}
		else
		{
			System.out.println("Using default core salt...");
		}
		
		File usersFile = new File("cfg/users.xml");
		
		//Memorize file contents, modifying passwords
		BufferedReader reader = new BufferedReader(new FileReader(usersFile));
		List<String> lines = new ArrayList<String>();
		
		Pattern pattern = Pattern.compile("(<\\s*password\\s*>)([^<]+)(<\\s*/\\s*password\\s*>)", Pattern.CASE_INSENSITIVE);
		int totalChanges = 0, skipped = 0;
		
		while (reader.ready())
		{
			String line = reader.readLine();
			Matcher matcher = pattern.matcher(line);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find())
			{
				String before = matcher.group(1);
				String password = matcher.group(2);
				String after = matcher.group(3);
				
				if (isLikeMD5(password))
				{
					skipped++;
					continue;
				}
				
				String digested = stringToMD5(password+salt);
				matcher.appendReplacement(buffer, before+digested+after);
				totalChanges++;
			}
			matcher.appendTail(buffer);
			
			lines.add(buffer.toString());
		}
		reader.close();
		
		System.out.println("Starting "+usersFile.getPath()+" file modification...");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(usersFile));
		
		for (String line : lines)
		{
			writer.write(line);
			writer.write("\r\n");
		}
		
		writer.close();
		
		System.out.println("Total changes: "+totalChanges);
		System.out.println("Encrypted passwords skipped: "+skipped);
	}

}
