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
package au.edu.unimelb.csse.search;

import org.apache.lucene.search.HitCollector;

public class SimpleHitCollector extends HitCollector {
	public int[] hits;
	public int totalHits;
	private int size;

	public SimpleHitCollector(int size) {
		hits = new int[size];
		this.size = size;
		totalHits = 0;
	}

	@Override
	public void collect(int doc, float score) {
		if (totalHits < size) {
			hits[totalHits++] = doc;
		} else {
			totalHits++;
		}
	}

	public int[] lastDocOfEachPage(int pageSize) {
		int arraySize = size / pageSize;
		if (totalHits < size) {
			arraySize = totalHits / pageSize;
		}
		final int[] lastDocs = new int[arraySize];
		for (int i = 0; i < arraySize; i++) {
			lastDocs[i] = hits[(i + 1) * pageSize - 1];
		}
		return lastDocs;
	}
	
	public void reset() {
		totalHits = 0;
	}

}
