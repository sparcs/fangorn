package au.edu.unimelb.csse.exp;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import au.edu.unimelb.csse.join.DocIter;

public class DocIterPerformanceTest {
	private DirectoryReader reader;
	private static final int TIMES = 6;

	public DocIterPerformanceTest(String indexDir) throws IOException {
		Directory directory = MMapDirectory.open(new File(indexDir));
		reader = DirectoryReader.open(directory);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			throw new IllegalArgumentException(
					"Program expects two parameters: <indexdir> <querynum>");
		}
		String indexDir = args[0];
		int queryNum = Integer.valueOf(args[1]);
		DocIterPerformanceTest test = new DocIterPerformanceTest(indexDir);
		TreeQuery query = new Queries().getQuery(queryNum);
		for (int i = 0; i < TIMES; i++) {
			test.run(query, queryNum);
		}
	}

	private void run(TreeQuery query, int queryNum) throws IOException {
		DocIter docIter = new DocIter(query.labels());
		int numDocs = 0;
		long startTime = System.nanoTime();
		docIter.setup(reader);
		while (docIter.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			numDocs++;
		}
		long endTime = System.nanoTime();
		System.out.println(queryNum + "," + numDocs + ","
				+ (endTime - startTime) / 1000000);
	}
}
