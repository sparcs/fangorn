package au.edu.unimelb.csse.exp;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import au.edu.unimelb.csse.join.DocPosIter;
import au.edu.unimelb.csse.paypack.LRDP;

public class DocPosIterPerformanceTest {
	private static final int TIMES = 6;
	private LRDP lrdp;
	private DirectoryReader reader;

	public DocPosIterPerformanceTest(String indexDir, LRDP lrdp) throws IOException {
		this.lrdp = lrdp;
		Directory directory = MMapDirectory.open(new File(indexDir));
		reader = DirectoryReader.open(directory);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			throw new IllegalArgumentException(
					"Program expects two parameters: <indexdir> <physicalpayloadformat> <querynum>");
		}
		String indexDir = args[0];
		LRDP lrdp = new LRDP(LRDP.PhysicalPayloadFormat.valueOf(args[1]));
		int queryNum = Integer.valueOf(args[2]);
		DocPosIterPerformanceTest test = new DocPosIterPerformanceTest(indexDir, lrdp);
		TreeQuery query = new Queries().getQuery(queryNum);
		for (int i = 0; i < TIMES; i++) {
			test.run(query, queryNum);
		}

	}

	private void run(TreeQuery query, int queryNum) throws IOException {
		DocPosIter docPosIter = new DocPosIter(query.labels(), lrdp);
		int numDocs = 0;
		long startTime = System.nanoTime();
		docPosIter.setup(reader);
		while (docPosIter.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
			docPosIter.readAllPositions();
			numDocs++;
		}
		long endTime = System.nanoTime();
		System.out.println(queryNum + "," + numDocs + ","
				+ (endTime - startTime) / 1000000);
	}
}
