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
package au.edu.unimelb.csse.search.complete;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;

import au.edu.unimelb.csse.search.NodeDataBuffer;
import au.edu.unimelb.csse.search.TreeExpr;
import au.edu.unimelb.csse.search.TreebankQuery;

public class AllResults {
	private int[] hits;
	private TreeExpr expr;
	private int numberOfResults;

	public AllResults(int[] hits, int numberOfFullResults, TreebankQuery query) {
		this.hits = hits;
		this.expr = query.getTreeExpr();
		this.numberOfResults = numberOfFullResults;
	}
	
	public Result[] collect(IndexSearcher searcher) throws IOException {
		expr.init(searcher.getIndexReader());
		Result[] r = new Result[numberOfResults];
		for (int i = 0; i < numberOfResults; i++) {
			expr.skipTo(hits[i]);
			Result result = new Result();
			byte[] payloadBuffer = new byte[NodeDataBuffer.BUFFER_SIZE * 4];
			int[] positionsBuffer = new int[NodeDataBuffer.BUFFER_SIZE];
			NodeDataBuffer data = new NodeDataBuffer();

			r[i] = expr.allStructureMatch(result, data, payloadBuffer, positionsBuffer);
		}
		return r;
	}
}
