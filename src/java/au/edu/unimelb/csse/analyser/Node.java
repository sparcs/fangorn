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
package au.edu.unimelb.csse.analyser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.index.Payload;

import au.edu.unimelb.csse.JSONAble;

public class Node implements JSONAble {
	private static String NONAME = "";
	String name;
	int left;
	int right;
	int depth;
	int parentId;
	List<Node> children = new ArrayList<Node>();
	
	public String asJSONString() {
		String[] childrenToJSON = new String[children.size()];
		for (int i = 0 ; i< children.size(); i++) {
			childrenToJSON[i] = children.get(i).asJSONString();
		}
		return "{\"n\":\"" + getName() + "\", \"i\":\"" + right + "_" + left + "_" + depth + "_" + parentId + "\", \"c\":" + Arrays.toString(childrenToJSON) + "}";
	}
		
	private String getName() {
		if (name.contains("\"")) {
			return name.replace("\"", "\\\"");
		}
		return name;
	}

	@Override
	public String toString() {
		return "(" + right + "," + left + "," + depth + "," + parentId + ")";
	}
	
	public Payload getPayload() {
		if (!(depth < 256 && left < 256 && right < 256 && parentId < 256)) {
			throw new OverflowException(
					"Exceeded allocated space for payload l=" + left + " r="
							+ right + " p=" + parentId + " d=" + depth);
		}

		byte[] v = new byte[] { (byte) (right & 255), (byte) (left & 255),
				(byte) (depth & 255), (byte) (parentId & 255) };

		return new Payload(v);

	}
	
	int totalNumberOfNodes() {
		int size = 1;
		for (Node child : children) {
			size += child.totalNumberOfNodes();
		}
		return size;
	}

	public void clear() {
		name = NONAME;
		left = right = depth = parentId = 0;
		children.clear();
	}

	public int[] getIntArray() {
		return new int[]{left, right, depth, parentId};
	}

	public String label() {
		return name;
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	public List<Node> children() {
		return children;
	}
	
	public boolean hasPositionGreaterThan256() {
		if (!(depth < 256 && left < 256 && right < 256 && parentId < 256)) {
			return true;
		}
		if (hasChildren()) {
			for (Node childNode : children) {
				if (childNode.hasPositionGreaterThan256()) {
					return true;
				}
			}
		}
		return false;
	}
}
