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
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import au.edu.unimelb.csse.analyser.Node;
import au.edu.unimelb.csse.analyser.NodeTreebankAnalyser;
import au.edu.unimelb.csse.analyser.OverflowException;
import au.edu.unimelb.csse.analyser.SentenceAndMetaData;
import au.edu.unimelb.csse.analyser.SentenceTokenizer;
import au.edu.unimelb.csse.analyser.String2NodesParser;

public class CreateIndex {
	private File dataLocation;
	private IndexWriter writer;
	private SentenceTokenizer tokenizer;
	private NodeTreebankAnalyser analyser;
	private int sentencesProcessed = 0;
	private int numSents;
	private boolean maxReached = false;
	private boolean gzip;
	private String indexName;
	private String2NodesParser parser = new String2NodesParser();
	private String indexDir;
	private static final Logger logger = Logger.getLogger(CreateIndex.class
			.getName());

	public CreateIndex(String indexDir, String dataDir, int numberOfSents,
			String indexName, boolean gzip) throws CorruptIndexException,
			LockObtainFailedException, IOException {
		analyser = new NodeTreebankAnalyser(false);
		writer = new IndexWriter("index" + File.separator + indexDir, analyser,
				true, IndexWriter.MaxFieldLength.UNLIMITED);
		this.indexDir = indexDir;
		this.dataLocation = new File(dataDir);
		this.numSents = numberOfSents;
		this.gzip = gzip;
		this.indexName = indexName.replaceAll("_", " ");
	}

	public void create() throws IOException {
		checkIfDirNameIsUnique();
		maxReached = false;
		if (!dataLocation.isDirectory()) {
			System.err.println("Data directory not found");
			return;
		}
		processDir(dataLocation, 1);
		writeInfoToDB();
	}

	private void checkIfDirNameIsUnique() {
		DB db = new DB();
		db.loadDriver();
		try {
			final boolean directoryUnique = db.noDirectoryByName(indexDir);
			if (!directoryUnique) {
				System.out.println("An index is already present at directory "
						+ indexDir + ". Please choose an unique index dir.");
				db.shutdown();
				System.exit(1);
			}
		} catch (SQLException e) {
			logger.warning(e.getMessage());
		}
		db.shutdown();
	}

	private void writeInfoToDB() {
		DB db = new DB();
		db.loadDriver();
		try {
			db.insert(indexDir, indexName, (int) sentencesProcessed);
		} catch (SQLException e) {
			logger.warning(e.getMessage());
		}
		db.shutdown();
	}

	private void processDir(File f, int level) throws IOException {
		if (f.isDirectory()) {
			System.out
					.println("[" + DateFormat.getInstance().format(new Date())
							+ "] Indexing contents of directory "
							+ f.getAbsolutePath());
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

	void processTreebankFile(File file) throws IOException {
		if (gzip) {
			if (!file.getName().endsWith(".gz"))
				return;
		} else {
			if (!file.getName().endsWith(".mrg"))
				return;
		}
		BufferedReader reader = new BufferedReader(getInputStreamReader(file));
		System.out.println("[" + DateFormat.getInstance().format(new Date())
				+ "] Indexing file " + file.getName());

		if (tokenizer == null) {
			tokenizer = new SentenceTokenizer(reader);
		} else {
			tokenizer.reset(reader);
		}
		SentenceAndMetaData next = tokenizer.next();
		while (next != null) {
			String sentence = next.sentence();
			Node root;
			try {
				root = parser.parse(sentence);
			} catch (ParseException e1) {
				e1.printStackTrace();
				next = tokenizer.next();
				continue;
			}
			String asJson = root.asJSONString();
			Document d = new Document();
			d.add(new Field("sent", asJson, Field.Store.COMPRESS,
					Field.Index.ANALYZED_NO_NORMS,
					Field.TermVector.WITH_POSITIONS));
			d.add(new Field("docnum", file.getName() + "." + next.lineOffset(),
					Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
			// String id = "n=" + fname + "&l=" + next.lineOffset() + "&nol"
			// + next.numberOfLines();
			try {
				writer.addDocument(d);
				sentencesProcessed++;
			} catch (OverflowException e) {
				// System.err.println("cannot index sentence " + id);
				// logger.info(e.getMessage());
				next = tokenizer.next();
				continue;
			} catch (Exception e) {
				// System.err.println("error while indexing sentence " + id);
				System.err.println(e.getMessage());
				logger.warning(e.getMessage());
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
		if (gzip)
			return new InputStreamReader(new GZIPInputStream(
					new FileInputStream(file)));
		else
			return new FileReader(file);
	}

	public long sentencesProcessed() {
		return sentencesProcessed;
	}

	public static void main(String[] args) throws CorruptIndexException,
			LockObtainFailedException, IOException {
		if (args.length != 5) {
			System.out.println("Your arguments were:");
			for (int i = 0; i < args.length; i++) {
				System.out.println((i + 1) + ". " + args[i]);
			}
			System.out
					.println("Usage: java CreateIndex <data_source> <index_dir_name> <index_name> <number_of_sents> <files_are_gzipped?(true/false)>");
			System.out
					.println("Enter -1 for <number_of_sents> if all sentences should be indexed.");
			System.out
					.println("Underscores in <index_name> will be replaced with spaces.");

			System.exit(1);
		}
		final String dataSource = args[0];
		CreateIndex ix = new CreateIndex(args[1], dataSource,
				Integer.parseInt(args[3]), args[2],
				Boolean.parseBoolean(args[4]));
		long start = System.currentTimeMillis();
		ix.create();
		ix.commit();
		long stop = System.currentTimeMillis();
		System.out.println("Total number of sentences indexed: "
				+ ix.sentencesProcessed());
		System.out.println("Total time (sec): " + ((stop - start) / 1000.0));
	}

	private void commit() throws CorruptIndexException, IOException {
		System.out.println("[" + DateFormat.getInstance().format(new Date())
				+ "] Optimizing Index.");
		writer.optimize();
		System.out.println("[" + DateFormat.getInstance().format(new Date())
				+ "] Committing Index.");
		writer.commit();
		writer.close();
	}
}
