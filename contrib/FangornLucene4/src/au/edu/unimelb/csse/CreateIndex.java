package au.edu.unimelb.csse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import au.edu.unimelb.csse.analyser.SentenceAndMetaData;
import au.edu.unimelb.csse.analyser.SentenceTokenizer;
import au.edu.unimelb.csse.analyser.TreeAnalyzer;
import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.LRDP;

public class CreateIndex {
	private String srcDirName;
	private SentenceTokenizer sentenceTokenizer;
	private IndexWriter writer;
	private AtomicInteger sentIndexedCount = new AtomicInteger(0);
	private FieldType ft;
//	private static final int BUF_SIZE = 510;
//	private static final int CPU_CORES = 4;
//	private static final int THREAD_POOL_SIZE = (int) Math
//			.round(CPU_CORES * 1.5);
	private FieldType docNumField;

	public CreateIndex(String srcDir, String indexDir) throws IOException {
		this.srcDirName = srcDir;
		sentenceTokenizer = new SentenceTokenizer(new BufferedReader(
				new StringReader("DUMMY")));

		Directory d = new MMapDirectory(new File(indexDir));
		Analyzer a = new TreeAnalyzer(new LRDP(new BytePacking(4)));
		IndexWriterConfig c = new IndexWriterConfig(Version.LUCENE_40, a);
		c.setRAMBufferSizeMB(1024);
		writer = new IndexWriter(d, c);
		ft = createFieldType();
		
		docNumField = new FieldType();
		docNumField.setTokenized(false);
		docNumField.setIndexed(false);
		docNumField.setStored(true);
	}

	/**
	 * This assumes a linux box with at least 8 GB of RAM
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			throw new IllegalArgumentException("Incorrect number of arguments");
		}
		CreateIndex createIndex = new CreateIndex(args[0], args[1]);
		createIndex.run();
	}

	private void run() throws IOException {
		String message = "Indexing files in ";
		File f = new File(srcDirName);
		if (f.isDirectory()) {
			message += "dir ";
		}
		System.out.println(message + srcDirName);
		long startTime = System.nanoTime();
		indexFile(f);
		writer.commit();
		writer.close();
		long endTime = System.nanoTime();
		System.out.println("Indexed " + sentIndexedCount.get()
				+ " sentences in " + (endTime - startTime) / 1000000000 + "s.");
	}

	private void indexFile(File f) {
		File[] listFiles = f.listFiles();
		if (listFiles == null) {
			Reader r;
			try {
				if (f.getName().endsWith(".gz")) {
					r = new BufferedReader(new InputStreamReader(
							new GZIPInputStream(new FileInputStream(f))));
				} else {
					r = new FileReader(f);
				}
				BufferedReader bufferedReader = new BufferedReader(r);
				sentenceTokenizer.reset(bufferedReader);
				SentenceAndMetaData sm;
				while ((sm = sentenceTokenizer.next()) != null) {
					// int bufIndex = 0;
					// String[] sents = new String[BUF_SIZE];
					// while (bufIndex < BUF_SIZE - 1 && sm != null) {
					// sents[bufIndex++] = sm.sentence();
					// sm = sentenceTokenizer.next();
					// }
					// if (sm != null) {
					// sents[bufIndex++] = sm.sentence();
					// }
					// ExecutorService executor =
					// Executors.newFixedThreadPool(THREAD_POOL_SIZE);
					// for (int i = 0; i < bufIndex; i++) {
					// executor.execute(new ExecutorThread(sents[i]));
					// }
					// executor.shutdown();
					// executor.awaitTermination(BUF_SIZE * 100,
					// TimeUnit.SECONDS);
					//
					String sentence = sm.sentence();
					Document doc = new Document();
					doc.add(new Field(Constants.FIELD_NAME, sentence, ft));
					doc.add(new Field("docnum", f.getName() + "."
							+ sm.lineOffset(), docNumField));
					try {
						writer.addDocument(doc);
						sentIndexedCount.getAndIncrement();
					} catch (IOException e) {
						// this catch block is necessary because a parse
						// exception is converted into an IOException while
						// adding doc
						System.err.println(e.getMessage());
					}
					// if (sm == null) {
					// break;
					// }
				}
			} catch (FileNotFoundException e) {
				System.err.println("File " + f.getAbsolutePath()
						+ " was not found.");
			} catch (IOException e) {
				System.err.println("Error reading file " + f.getAbsolutePath()
						+ ".");
//			} catch (InterruptedException e) {
//				System.err
//						.println("Interrupted while waiting for concurrent indexing to complete.");
			}
		} else {
			for (File sf : listFiles) {
				indexFile(sf);
			}
		}
	}

	private FieldType createFieldType() {
		FieldType ft = new FieldType();
		ft.setIndexed(true);
		ft.setTokenized(true);
		ft.setOmitNorms(true);
		ft.freeze();
		return ft;
	}

	class ExecutorThread implements Runnable {

		private String sentence;

		public ExecutorThread(String sentence) {
			this.sentence = sentence;
		}

		@Override
		public void run() {
			Document doc = new Document();
			doc.add(new Field(Constants.FIELD_NAME, sentence, ft));
			try {
				writer.addDocument(doc);
				sentIndexedCount.incrementAndGet();
			} catch (IOException e) {
				// this catch block is necessary because a parse
				// exception is converted into an IOException while
				// adding doc
				System.err.println(e.getMessage());
			}
		}

	}
}
