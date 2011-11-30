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
package au.edu.unimelb.csse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import au.edu.unimelb.csse.analyser.Node;
import au.edu.unimelb.csse.analyser.SentenceAndMetaData;
import au.edu.unimelb.csse.analyser.SentenceTokenizer;
import au.edu.unimelb.csse.analyser.String2NodesParser;

public class PayloadsListing {
	private File dataSource;
	private BufferedWriter dest;
	int sentencesProcessed = 0;
	private SentenceTokenizer tokenizer;
	private String2NodesParser parser = new String2NodesParser();

	public PayloadsListing(String dataDir, String destFileName) throws IOException {
		dataSource = new File(dataDir);
		dest = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(destFileName))));
	}

	public static void main(String[] args) throws IOException {
		PayloadsListing listing = new PayloadsListing(
				"/home/sumukh/Data/wiki-ccg-sents-processed",
				"/home/sumukh/Data/wikilisting");
		long start = System.currentTimeMillis();
		listing.create();
		long stop = System.currentTimeMillis();
		System.out.println("total number of sentences indexed: "
				+ listing.sentencesProcessed);
		System.out.println("total time (sec): " + ((stop - start) / 1000.0));
	}

	private void create() throws IOException {
		if (!dataSource.isDirectory()) {
			return;
		}
		processDir(dataSource, 1);
		
		dest.close();
	}

	private void processDir(File f, int level) throws IOException {
		if (level > 1 && f.isDirectory()) {
			System.out.println("Processing directory " + f.getName());
		}
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File fi : files) {
				if (fi.isDirectory()) {
					processDir(fi, level + 1);
				} else {
					processTreebankFile(fi);
				}
			}
		}
	}

	void processTreebankFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		if (tokenizer == null) {
			tokenizer = new SentenceTokenizer(reader);
		} else {
			tokenizer.reset(reader);
		}
		SentenceAndMetaData next = tokenizer.next();
		int numberParsed = 0;
		while (next != null) {
			String sentence = next.sentence();
			numberParsed++;
			Node root;
			try {
				root = parser.parse("(" + sentence + ")");
			} catch (ParseException e1) {
				System.err.println("Error parsing " + sentence + " in file " + file.getAbsolutePath());
				System.err.println(numberParsed + " sentences before this have been parsed successfully");
				next = tokenizer.next(); 
				continue;
			}
			Map<String, List<int[]>> positions = new HashMap<String, List<int[]>>();
			boolean withinLimit = addPositions(positions, root);
			if (withinLimit) {
				Set<String> ls = positions.keySet();
				
				List<String> labels = new ArrayList<String>(ls);
				Collections.sort(labels);
				for (String label : labels) {
					List<int[]> ps = positions.get(label);
					for (int[] p : ps) {
						dest.write((sentencesProcessed + 1) + "\t" + label + "\t" + p[0] + "\t" + p[1] + "\t" + p[2] + "\t" + p[3] + "\n");
					}
				}
				sentencesProcessed++;
			}
			next = tokenizer.next();
		}
		reader.close();
	}

	private boolean addPositions(Map<String, List<int[]>> positions, Node node) {
		int[] intArray = node.getIntArray();
		for (int pos : intArray) {
			if (pos > 255) {
				return false;
			}
		}
		String label = node.label();
		if (!positions.containsKey(label)) {
			positions.put(label, new ArrayList<int[]>());
		}
		positions.get(label).add(intArray);
		boolean returnValue = true;
		if (node.hasChildren()) {
			List<Node> children = node.children();
			int i = 0;
			while (i < children.size() && returnValue) {
				returnValue = returnValue & addPositions(positions, children.get(i++));
			}
		}
		return returnValue;
	}

}
