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
package au.edu.unimelb.csse.join;

import java.util.Arrays;

import junit.framework.TestCase;

public class ByteTricksTest extends TestCase{
	public void testByteToIntConversions() throws Exception {
		int i = 229;
		byte t = (byte) i;
		int j = t;
		assertEquals(-27, j);
		int k = t & 255;
		assertEquals(229, k);
		int l = (t | 256) & 255;
		assertEquals(229, l);
		
		
		i = 128;
		t = (byte) i;
		j = t;
		assertEquals(-128, j);
		k = t & 255;
		assertEquals(128, k);
		l = (t | 256) & 255;
		assertEquals(128, l);

	}
	
	public void testRightShiftByteTricks() throws Exception {
		int plainIntValue = 0xC8;
		int andedIntValue = 0xC8 & 255;
		int payload = 200 & 255;
		assertEquals(200, plainIntValue);
		assertEquals(200, andedIntValue);
		assertEquals(200, payload);
		payload = payload << 24;
		int plainNewPayload = payload >> 24;
		int andedNewPayload = (payload >> 24) & 255;
		byte plainByte = (byte) (((payload & (255 << 24)) >> 24) & 255);
		byte[] bytes = new byte[]{plainByte};
		assertEquals(-56, plainNewPayload);
		assertEquals(200, andedNewPayload);
		assertEquals("[-56]", Arrays.toString(bytes));
	}
}
