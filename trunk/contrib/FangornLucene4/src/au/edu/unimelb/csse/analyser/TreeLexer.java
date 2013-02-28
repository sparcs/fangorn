package au.edu.unimelb.csse.analyser;
/*******************************************************************************
 * Copyright 2011 The fangorn project
 * 
 *        Author: Sumukh Ghodke
 * 
 *        Licensed to the Apache Software Foundation (ASF) under one
 *        or more contributor license agreements.  See the NOTICE file
 *        distributed with this work for additional information
 *        regarding copyright ownership.  The ASF licenses this file
 *        to you under the Apache License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance
 *        with the License.  You may obtain a copy of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *        Unless required by applicable law or agreed to in writing,
 *        software distributed under the License is distributed on an
 *        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *        KIND, either express or implied.  See the License for the
 *        specific language governing permissions and limitations
 *        under the License.
 ******************************************************************************/


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeLexer {
	private Pattern pattern = Pattern.compile("[^\\(\\) ]+|\\(|\\)|\\s+");
	private Matcher matcher = pattern.matcher("");
	private int[] ints = new int[512];

	int[] tokenMarkerPos(String sentence) {
		matcher.reset(sentence);
		int index = 0;
		int end = 0;
		int start = 0;
		while (end < sentence.length()) {
			if (matcher.find()) {
				start = matcher.start();
				end = matcher.end();
			} else {
				start = end;
				end = sentence.length();
			}
			try {
				ints[index++] = start;
			} catch (ArrayIndexOutOfBoundsException e) {
				expandIntsArrayAndWriteValue(index, start);
			}
			try {
				ints[index++] = end;
			} catch (ArrayIndexOutOfBoundsException e) {
				expandIntsArrayAndWriteValue(index, end);
			}
		}
		int[] r = new int[index];
		System.arraycopy(ints, 0, r, 0, index);
		return r;
	}

	private void expandIntsArrayAndWriteValue(int index, int value) {
		int[] oldints = ints;
		ints = new int[ints.length + 512];
		System.arraycopy(oldints, 0, ints, 0, oldints.length);
		ints[index - 1] = value;
	}

}
