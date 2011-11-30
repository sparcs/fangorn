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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import au.edu.unimelb.csse.analyser.FastStringAnalyser;
import au.edu.unimelb.csse.analyser.OverflowException;
import au.edu.unimelb.csse.analyser.SentenceAndMetaData;
import au.edu.unimelb.csse.analyser.SentenceTokenizer;

public class CreateTextIndex {
	private File dataDir;
	private Integer numSents;
	private IndexWriter writer;
	private boolean isGzip;
	private boolean maxReached;
	private SentenceTokenizer tokenizer;
	private int sentencesProcessed;
	private long stopTime;
	private long startTime;

	public CreateTextIndex(String indexDir, String dataDir, boolean isGzip,
			Integer numOfSents) throws CorruptIndexException,
			LockObtainFailedException, IOException {
		writer = new IndexWriter(indexDir, new FastStringAnalyser(), true,
				IndexWriter.MaxFieldLength.UNLIMITED);
		this.dataDir = new File(dataDir);
		this.numSents = numOfSents;
		this.isGzip = isGzip;
		this.sentencesProcessed = 0;
	}

	public static void main(String[] args) {
		if (args.length != 4) {
			System.out
					.println("Usage: java -cp <classpath> au.edu.unimelb.csse.CreateTextIndex <index_dir> <data_dir> <is_gzip> <no_of_sents>");
			System.exit(1);
		}
		CreateTextIndex createIndex = null;
		try {
			createIndex = new CreateTextIndex(args[0], args[1], Boolean
					.parseBoolean(args[2]), Integer.parseInt(args[3]));
			System.out.println("Index dir:" + args[0] + "\ntime:" + DateFormat.getInstance().format(new Date()));
			createIndex.setStartTime();
			
			createIndex.create();
			createIndex.close();
			createIndex.setStopTime();
			
			System.out.println("total number of sentences indexed: "
					+ createIndex.sentencesProcessed);
			System.out
					.println("total time (sec): " + ((createIndex.getStopTime() - createIndex.getStartTime()) / 1000.0));

		} catch (CorruptIndexException e) {
			System.out.println("Error creating index." + e.getMessage());
			System.exit(1);
		} catch (LockObtainFailedException e) {
			System.out.println("Error creating index." + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Error creating index." + e.getMessage());
			System.exit(1);
		}
	}

	private long getStopTime() {
		return stopTime;
	}

	private long getStartTime() {
		return startTime;
	}

	private void setStopTime() {
		stopTime = System.currentTimeMillis();
	}

	private void setStartTime() {
		startTime = System.currentTimeMillis();
	}

	private void create() throws IOException {
		maxReached = false;
		if (!dataDir.isDirectory()) {
			System.err.println("Data directory not found");
			return;
		}
		processDir(dataDir, 1);

	}

	private void processDir(File f, int level) throws IOException {
		if (f.isDirectory()) {
			System.out.println("Processing directory " + f.getName());
			File[] files = f.listFiles();
			for (File fi : files) {
				if (maxReached)
					break;
				if (fi.isDirectory()) {
					processDir(fi, level + 1);
				} else {
					processTreebankFile(fi);
					if (maxReached)
						break;
				}
			}
		}
	}

	private void processTreebankFile(File file) throws IOException {
		if (isGzip) {
			if (!file.getName().endsWith(".gz"))
				return;
		} else {
			if (!file.getName().endsWith(".mrg"))
				return;
		}
		BufferedReader reader = new BufferedReader(getInputStreamReader(file));
		System.out.println("Processing treebank file: " + file.getName());
		if (tokenizer == null) {
			tokenizer = new SentenceTokenizer(reader);
		} else {
			tokenizer.reset(reader);
		}
		SentenceAndMetaData next = tokenizer.next();
		while (next != null) {
			String sentence = next.sentence();
			Document d = new Document();
			d.add(new Field("sent", sentence, Field.Store.NO,
					Field.Index.ANALYZED_NO_NORMS,
					Field.TermVector.WITH_POSITIONS));
			try {
				writer.addDocument(d);
				sentencesProcessed++;
				if (sentencesProcessed % 500000 == 0) {
					System.out.println("Finished indexing " + sentencesProcessed + " sentences.");
					System.out.println("Time from start: " + (System.currentTimeMillis() - startTime)/ 1000.0 + " seconds.");
				}
			} catch (OverflowException e) {
				next = tokenizer.next();
				continue;
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			if (sentencesProcessed == numSents) {
				maxReached = true;
				break;
			}
			next = tokenizer.next();
		}
		reader.close();
	}

	private InputStreamReader getInputStreamReader(File file)
			throws IOException, FileNotFoundException {
		if (isGzip)
			return new InputStreamReader(new GZIPInputStream(
					new FileInputStream(file)));
		else
			return new FileReader(file);
	}

	private void close() throws CorruptIndexException, IOException {
		writer.optimize();
		writer.commit();	
		writer.close();
	}

}
