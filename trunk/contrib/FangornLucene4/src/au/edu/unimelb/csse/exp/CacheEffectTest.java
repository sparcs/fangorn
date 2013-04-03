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
import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.BytePacking2212;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class CacheEffectTest {
	private static final LogicalNodePositionAware LRDP = new LRDP(
			new BytePacking2212());
	private static final String INDEX_DIR = "/opt/wiki-index-all";
	private static final int TIMES = 6;
	private IndexReader reader;
	
	public CacheEffectTest() throws IOException {
		Directory directory = MMapDirectory.open(new File(INDEX_DIR));
		reader = DirectoryReader.open(directory);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			throw new IllegalArgumentException(
					"Program expects two parameters: <jointype> <querynum>");
		}
		String joinTypeString = args[0];
		JoinType joinType = JoinType.valueOf(joinTypeString);
		int queryNum = Integer.valueOf(args[1]);
		CacheEffectTest test = new CacheEffectTest();
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
				execute(joinType.getFullJoin(query, LRDP), reader, joinType, queryNum);
			} else {
				execute(joinType.getBooleanJoin(query, LRDP), reader, joinType, queryNum);
			}
		}

	}

	private void execute(ComputesBooleanResult join,
			IndexReader r, JoinType joinType, int queryId) throws IOException {
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
		System.out.println(queryId + "," + joinType.getId() + "," + numResultDocs + ","
				+ (endTime - startTime) / 1000000);
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
		System.out.println(queryId + "," + joinType.getId() + "," + numResultDocs + ","
				+ (endTime - startTime) / 1000000 + "," + totalMatches);
	}
}
