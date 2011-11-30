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
package au.edu.unimelb.csse.search.join;

import au.edu.unimelb.csse.search.NodeDataBuffer;

public class DoNotUseJoinLogic extends AbstractJoinLogic{

	@Override
	public boolean joinBufWithBuf(NodeDataBuffer buffer1, int pos1,
			NodeDataBuffer buffer2, int pos2) {
		return false;
	}

	@Override
	public boolean joinBufWithBytes(NodeDataBuffer data, int dataPtr,
			byte[] buffer, int bufferPtr) {
		return false;
	}

	@Override
	public boolean joinBytesWithBytes(byte[] start, int startStartPos,
			byte[] end, int endStartPos) {
		return false;
	}

}
