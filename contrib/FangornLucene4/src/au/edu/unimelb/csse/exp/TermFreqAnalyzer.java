package au.edu.unimelb.csse.exp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import au.edu.unimelb.csse.Constants;
import au.edu.unimelb.csse.join.DocIter;

public class TermFreqAnalyzer {

	private DirectoryReader reader;
	private TreeQuery query;

	public TermFreqAnalyzer(DirectoryReader reader, TreeQuery query) {
		this.reader = reader;
		this.query = query;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err
					.println("Incorrect number of arguments; expected <index_dir> <query_number>");
		}
		String indexDir = args[0];
		int i = Integer.parseInt(args[1]);
		Queries queries = new Queries();
		Directory directory = MMapDirectory.open(new File(indexDir));
		DirectoryReader reader = DirectoryReader.open(directory);
		TermFreqAnalyzer termFreqAnalyzer = new TermFreqAnalyzer(reader,
				queries.getQuery(i));
		termFreqAnalyzer.printAvgTf();
		termFreqAnalyzer.printAvgTfInDocsWithAllTerms();
		reader.close();
//		directory.close();
	}

	private void printAvgTfInDocsWithAllTerms() throws IOException {
		DocIter docIter = new DocIter(query.labels());
		int numDocs = 0;
		docIter.setup(reader);
		int numQueryTerms = query.labels().length;
		long[] totals = new long[numQueryTerms];
		Arrays.fill(totals, 0);
		while (docIter.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			numDocs++;
			for (int i = 0; i < numQueryTerms; i++) {
				totals[i] += docIter.getTermFreq(i);
			}
		}
		if (numQueryTerms > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append(totals[0] * 1.0 / numDocs);
			for (int j = 0; j < numQueryTerms; j++) {
				builder.append(",");
				builder.append(totals[j] * 1.0 / numDocs);
			}
			System.out.println(builder.toString());
		}
	}

	private void printAvgTf() throws IOException {
		double[] avgtf = new double[query.labels().length];
		int i = 0;
		for (String label : query.labels()) {
			Term term = new Term(Constants.FIELD_NAME, label);
			long totalTf = reader.totalTermFreq(term);
			long docf = reader.docFreq(term);
			avgtf[i++] = totalTf * 1.0 / docf;
		}
		if (avgtf.length > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append(avgtf[0]);
			for (i = 1; i < avgtf.length; i++) {
				builder.append(",");
				builder.append(avgtf[i]);
			}
			System.out.println(builder.toString());
		}
	}
}
