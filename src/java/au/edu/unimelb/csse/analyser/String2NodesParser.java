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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Token;

import au.edu.unimelb.csse.ParseException;

public class String2NodesParser {
	private static final Pattern PATTERN =  Pattern.compile("\\)|\\([^()]*");
	private static final Matcher MATCHER = PATTERN.matcher("");
	private Stack<Node> s = new Stack<Node>();
	private List<Node> nodes = new ArrayList<Node>();
	private int right, parentId;
	private boolean found;
	private int nodesPosition;
	
	public Node parse(String sentence) throws ParseException {
		if (null == sentence) return null;
		Node root = null;
		right = 0;
		parentId = -1;
		MATCHER.reset(sentence);
		found = MATCHER.find();
		if (nodes.size() > 0) {
			NodeCache.returnNodeDeep(nodes.get(0));
			nodes.clear();
		}
		nodesPosition = -1;
		while (found) {
			if (MATCHER.end() - MATCHER.start() == 1
					&& sentence.charAt(MATCHER.start()) == ')') {
				if (s.isEmpty()) {
					throw new ParseException("cannot parse " + sentence);
				}
				Node n = s.pop();
				if (n.children.size() == 0) {
					throw new ParseException("cannot parse " + sentence);
				}				
				n.left = n.children.get(0).left;
				n.right = n.children.get(n.children.size() - 1).right;
				for (Node child : n.children) {
					child.parentId = parentId + 1;
				}
				parentId++;
				root = n;
			} else {
				Node node = NodeCache.get();
				String sub = sentence.substring(MATCHER.start(), MATCHER.end());
				String[] split = sub.split(" ");
				if (split.length == 1) {
					if (split[0].charAt(0) == '(') {
						node.name = split[0].substring(1);
					} else if (split[0].trim().charAt(0) == '(') {
						node.name = split[0].trim().substring(1);
					} else {
						throw new ParseException("cannot parse " + sentence);
					}
					if (!s.isEmpty()) {
						s.peek().children.add(node);
					}
					s.add(node);
					node.depth = s.size() - 1;
				} else {
					String first, last;
					first = split[0];
					if (split.length == 2) {
						last = split[1];
					} else {
						last = split[split.length - 1];
					}
					if (first.charAt(0) == '(') {
						node.name = first.substring(1);
					} else if (first.trim().charAt(0) == '(') {
						node.name = first.trim().substring(1);
					} else {
						throw new ParseException("cannot parse " + sentence);
					}
					Node newNode = NodeCache.get();
					String trimmed = last.trim();
					newNode.name = trimmed;
					newNode.left = right;
					newNode.right = newNode.left + 1;
					right = newNode.right;
					node.children.add(newNode);
					if (!s.isEmpty()) {
						s.peek().children.add(node);
					}
					s.add(node);
					node.depth = s.size() - 1;
					newNode.depth = s.size();
				}
			}
			found = MATCHER.find();
		}
		if (!s.isEmpty()) {
			throw new ParseException("cannot parse " + sentence);
		}
		if (root != null) {
			root.parentId = parentId + 1;
			makeList(root);
		}
		return root;
	}
	
	private void makeList(Node node) {
		nodes.add(node);
		for (Node child : node.children) {
			makeList(child);
		}
	}
	
	public Token next(final Token reusableToken) throws IOException {
		assert reusableToken != null;
		nodesPosition++;
		if (nodesPosition < nodes.size()) {
			reusableToken.clear();
			Node node = nodes.get(nodesPosition);
			reusableToken.setTermBuffer(node.name);
			reusableToken.setPayload(node.getPayload());
			return reusableToken;
		}
		return null;
	}

}
