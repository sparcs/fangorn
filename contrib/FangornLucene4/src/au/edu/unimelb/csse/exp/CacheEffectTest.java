package au.edu.unimelb.csse.exp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import au.edu.unimelb.csse.join.ComputesBooleanResult;
import au.edu.unimelb.csse.join.ComputesFullResults;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LRDP.PhysicalPayloadFormat;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class CacheEffectTest {
	private final LogicalNodePositionAware lrdp;
	private static final int TIMES = 5;
	private IndexReader reader;

	public CacheEffectTest(String indexDir, PhysicalPayloadFormat ppf) throws IOException {
		lrdp = new LRDP(ppf);
		Directory directory = MMapDirectory.open(new File(indexDir));
		reader = DirectoryReader.open(directory);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			throw new IllegalArgumentException(
					"Program expects two parameters: <indexdir> <jointype> <querynum> <physicalbyteformat>");
		}
		String indexDir = args[0];
		String joinTypeString = args[1];
		JoinType joinType = JoinType.valueOf(joinTypeString);
		int queryNum = Integer.valueOf(args[2]);
		LRDP.PhysicalPayloadFormat ppf = LRDP.PhysicalPayloadFormat
				.valueOf(args[3]);
		CacheEffectTest test = new CacheEffectTest(indexDir, ppf);
		test.run(joinType, queryNum);

	}

	private void run(JoinType joinType, int queryNum) throws IOException {
		TreeQuery query = new Queries().getQuery(queryNum);
		if (query.hasBranches() && !joinType.allowsBranches()) {
			return;
		}
		boolean returnsFullResults = joinType.returnsFullResults();
		for (int i = 0; i < TIMES; i++) {
			if (returnsFullResults) {
				execute(joinType.getFullJoin(query, lrdp), reader, joinType,
						queryNum);
			} else {
				execute(joinType.getBooleanJoin(query, lrdp), reader, joinType,
						queryNum);
			}
		}

	}

	private void execute(ComputesBooleanResult join, IndexReader r,
			JoinType joinType, int queryId) throws IOException {
		int numResultDocs = 0;
		int docId = -1;
		long startTime = System.nanoTime();
		join.setup(r);
		docId = join.nextDoc();
		while (docId != DocIdSetIterator.NO_MORE_DOCS) {
			boolean matches = join.match();
			if (matches) {
				numResultDocs++;
			}
			docId = join.nextDoc();
		}
		long endTime = System.nanoTime();
		System.out.println(queryId + "," + joinType.getId() + ","
				+ numResultDocs + "," + (endTime - startTime) / 1000000);
	}

	private void execute(ComputesFullResults join, IndexReader r,
			JoinType joinType, int queryId) throws IOException {
		int numResultDocs = 0;
		int totalMatches = 0;
		int docId = -1;
		long startTime = System.nanoTime();
		join.setup(r);
		docId = join.nextDoc();
		while (docId != DocIdSetIterator.NO_MORE_DOCS) {
			List<int[]> matches = join.match();
			int size = matches.size();
			if (size > 0) {
				numResultDocs++;
				totalMatches += size;
			}
			docId = join.nextDoc();
		}
		long endTime = System.nanoTime();
		System.out.println(queryId + "," + joinType.getId() + ","
				+ numResultDocs + "," + (endTime - startTime) / 1000000 + ","
				+ totalMatches);
	}
}
