package au.edu.unimelb.csse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.zip.GZIPInputStream;

import au.edu.unimelb.csse.analyser.SentenceAndMetaData;
import au.edu.unimelb.csse.analyser.SentenceTokenizer;
import au.edu.unimelb.csse.analyser.TreeTokenizer;

public class ListIndexedSents {
	private String srcDirName;
	private SentenceTokenizer sentenceTokenizer;
	private BufferedWriter writer;
	private int sentIndexedCount = 0;
	private TreeTokenizer tokenizer = new TreeTokenizer(new StringReader(""));
	private String sentenceFile;

	public ListIndexedSents(String srcDir, String sentenceFile)
			throws IOException {
		this.srcDirName = srcDir;
		sentenceTokenizer = new SentenceTokenizer(new BufferedReader(
				new StringReader("DUMMY")));
		this.sentenceFile = sentenceFile;
		writer = new BufferedWriter(new FileWriter(new File(sentenceFile)));
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			throw new IllegalArgumentException("Incorrect number of arguments");
		}
		new ListIndexedSents(args[0], args[1]).run();
	}

	private void run() throws IOException {
		String message = "Looking for files in ";
		File f = new File(srcDirName);
		if (f.isDirectory()) {
			message += "dir ";
		}
		System.out.println(message + srcDirName);
		long startTime = System.nanoTime();
		indexFile(f);
		long endTime = System.nanoTime();
		System.out.println("Wrote " + sentIndexedCount + " sentences to "
				+ sentenceFile + " in " + (endTime - startTime) / 1000000000
				+ "s.");
		writer.close();
		
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
					String sentence = sm.sentence();
					try {
						tokenizer.setReader(new StringReader(sentence));
						tokenizer.reset();
						// no exception so far indicates all is good
						sentIndexedCount++;
						writer.write("(" + sentence + ")\n");
					} catch (IOException e) {

					}
				}
				bufferedReader.close();
			} catch (FileNotFoundException e) {
				System.err.println("File " + f.getAbsolutePath()
						+ " was not found.");
			} catch (IOException e) {
				System.err.println("Error reading file " + f.getAbsolutePath()
						+ ".");
			}
		} else {
			for (File sf : listFiles) {
				indexFile(sf);
			}
		}
	}
}
