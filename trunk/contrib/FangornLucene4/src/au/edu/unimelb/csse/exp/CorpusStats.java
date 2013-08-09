package au.edu.unimelb.csse.exp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import au.edu.unimelb.csse.analyser.SentenceAndMetaData;
import au.edu.unimelb.csse.analyser.SentenceTokenizer;

public class CorpusStats {
	private String suffix;
	private String rootPath;
	private ExecutorService executor;
	private final AtomicInteger sentCount = new AtomicInteger(0);
	private final AtomicLong wordCount = new AtomicLong(0);
	private final AtomicLong annoCount = new AtomicLong(0);
	private final Map<Integer, Integer> wordDistribution = new HashMap<Integer, Integer>();
	private final Map<Integer, Integer> annoDistribution = new HashMap<Integer, Integer>();
	private final Map<String, Integer> uWordDistribution = new HashMap<String, Integer>();
	private final Map<String, Integer> uAnnoDistribution = new HashMap<String, Integer>();	

	public CorpusStats(String path) {
		rootPath = path;
		executor = Executors.newFixedThreadPool(3);
	}
	
	public CorpusStats(String path, String suffix) {
		this(path);
		this.suffix = suffix;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException,
			IOException {
		CorpusStats stats;
		if (args.length > 1) {
			stats = new CorpusStats(args[0], args[1]);
		} else {
			stats = new CorpusStats(args[0]);
		}
		stats.compute();
	}

	private void compute() throws InterruptedException, IOException {
		File f = new File(rootPath);
		processFile(f);
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.HOURS);
		System.out.println("number of sentences: " + sentCount.get());
		System.out.println("total number of words: " + wordCount.get());
		System.out.println("number of unique words: " + uWordDistribution.keySet().size());
		System.out.println("total number of annotations: " + annoCount.get());
		System.out.println("number of unique annotations: " + uAnnoDistribution.keySet().size());
		System.out.println("average number of annotations in a sentence: "
				+ (annoCount.get() * 1.0) / sentCount.get());
		System.out.println("average number of words in a sentence: "
				+ (wordCount.get() * 1.0) / sentCount.get());
		if (suffix != "") {
			suffix = "." + suffix;
		}
		writeDistribution("annoDist" + suffix, annoDistribution);
		writeDistribution("wordDist" + suffix, wordDistribution);
		writeWordDist("uniqueAnnoDist" + suffix, uAnnoDistribution);
		writeWordDist("uniqueWordDist" + suffix, uWordDistribution);
		
	}

	private void writeWordDist(String fileName,
			Map<String, Integer> uniqueDist) throws IOException {
		List<Entry<String, Integer>> list = new ArrayList<Map.Entry<String,Integer>>();
		list.addAll(uniqueDist.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1,
					Entry<String, Integer> o2) {
				if (o2.getValue() > o1.getValue()) return -1;
				if (o2.getValue() < o1.getValue()) return 1;
				return o1.getKey().compareTo(o2.getKey());
			}
		});
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				fileName)));
		for (Entry<String, Integer> entry : list) {
			writer.write(entry.getKey() + '\t' + entry.getValue() + '\n');
		}
		writer.close();
	}

	private void writeDistribution(String fileName,
			Map<Integer, Integer> dist) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				fileName)));
		Set<Integer> keySet = dist.keySet();
		List<Integer> sortedKeys = new ArrayList<Integer>();
		sortedKeys.addAll(keySet);
		Collections.sort(sortedKeys);
		for (Integer key : sortedKeys) {
			writer.write(key.toString() + "\t" + dist.get(key).toString() + "\n");
		}
		writer.close();
	}

	private void processFile(File f) {
		File[] files = f.listFiles();
		if (files == null) {
			executor.execute(new StatThread(f));
		} else {
			for (File file : files)
				processFile(file);
		}
	}

	public class StatThread implements Runnable {
		private File file;
		int sentNumWords;
		int sentNumAnno;
		Map<String, Integer> uniqueWordDistribution = new HashMap<String, Integer>();
		Map<String, Integer> uniqueAnnotationDistribution = new HashMap<String, Integer>();

		public StatThread(File f) {
			file = f;
		}

		@Override
		public void run() {
			try {
				SentenceTokenizer sentTokenizer = null;
//				if (file.getName().endsWith(".gz")) {
//					sentTokenizer = new SentenceTokenizer(new BufferedReader(new GZIPInputStream(new FileInputStream(file))));
//				} else {
					sentTokenizer = new SentenceTokenizer(new BufferedReader(
							new FileReader(file)));
//				}
				System.out.println("Processing file " + file.getAbsolutePath());
				SentenceAndMetaData sm;
				try {
					while ((sm = sentTokenizer.next()) != null) {
						String sentence = sm.sentence();
						Node root = processSent(sentence);
						if (root == null) {
							System.out.println("Error at line " + sm.lineOffset() + " in " + file.getName());
							continue;
						}
						sentNumWords = 0;
						sentNumAnno = 0;
						parse(root);
						wordCount.addAndGet(sentNumWords);
						annoCount.addAndGet(sentNumAnno);
						sentCount.incrementAndGet();
						synchronized (CorpusStats.class) {
							int oldValue = 0;
							if (wordDistribution.containsKey(sentNumWords)) {
								oldValue = wordDistribution.get(sentNumWords);
							}
							wordDistribution.put(sentNumWords, oldValue + 1);
					
							oldValue = 0;
							if (annoDistribution.containsKey(sentNumAnno)) {
								oldValue = annoDistribution.get(sentNumAnno);
							}
							annoDistribution.put(sentNumAnno, oldValue + 1);
						}
					}
					synchronized (CorpusStats.class) {
						for (Entry<String, Integer> entry : uniqueAnnotationDistribution.entrySet()) {
							int count = 0;
							if (uAnnoDistribution.containsKey(entry.getKey())) {
								count = uAnnoDistribution.get(entry.getKey());
							}
							uAnnoDistribution.put(entry.getKey(), count + entry.getValue());
						}
						for (Entry<String, Integer> entry : uniqueWordDistribution.entrySet()) {
							int count = 0;
							if (uWordDistribution.containsKey(entry.getKey())) {
								count = uWordDistribution.get(entry.getKey());
							}
							uWordDistribution.put(entry.getKey(), count + entry.getValue());
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		public void parse(Node node) {
			if (node.children.size() > 0) {
				sentNumAnno++;
				int count = 0;
				if (uniqueAnnotationDistribution.containsKey(node.label)) {
					count = uniqueAnnotationDistribution.get(node.label);
				}
				uniqueAnnotationDistribution.put(node.label, count + 1);
				for (Node child : node.children) {
					parse(child);
				}
			} else {
				int count = 0;
				if (uniqueWordDistribution.containsKey(node.label)) {
					count = uniqueWordDistribution.get(node.label);
				}
				uniqueWordDistribution.put(node.label, count + 1);
				sentNumWords++;
			}
		}

		public Node processSent(String sentence) {
			sentence = sentence.trim();
			sentence = sentence.replaceFirst("\\(", "");
			sentence = sentence.replaceAll("\\)$", "");

			Node root = null;
			Node current = null;
			int bracketDiff = 0;
			String currentLabel = "";
			char prev = '\0';
			boolean word = false;
			for (char c : sentence.toCharArray()) {
				if (c == '(') { // start new node
					Node newCurrent = new Node(current);
					if (root == null) {
						root = newCurrent;
					}
					if (current != null) {
						if (currentLabel != "") {
							current.label = currentLabel;
							currentLabel = "";
						}
						current.children.add(newCurrent);
					}
					current = newCurrent;
					bracketDiff++;
					prev = '(';
				} else if (c == ')') {
					if (currentLabel != "") {
						current.label = currentLabel;
					}
					current = current.parent;
					if (word) {
						current = current.parent;
						word = false;
					}
					currentLabel = "";
					bracketDiff--;
					prev = ')';
				} else if (c == ' ') {
					if (prev == 'T') {
						prev = ' ';
					}
				} else {
					if (prev == ' ') {
						current.label = currentLabel;
						Node newCurrent = new Node(current);
						current.children.add(newCurrent);
						current = newCurrent;
						currentLabel = "";
						word = true;
					}
					currentLabel = currentLabel + c;
					prev = 'T';
				}
			}
			if (current != null || bracketDiff != 0) {
				return null;
			}
			return root;
		}
	}

	class Node {
		String label;
		List<Node> children = new ArrayList<CorpusStats.Node>();
		Node parent;

		Node(Node parent) {
			this.parent = parent;
		}
	}

}
