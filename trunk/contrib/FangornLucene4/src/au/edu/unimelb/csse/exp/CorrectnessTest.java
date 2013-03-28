package au.edu.unimelb.csse.exp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import au.edu.unimelb.csse.join.ComputesBooleanResult;
import au.edu.unimelb.csse.join.ComputesFullResults;
import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public class CorrectnessTest {
	private static final LogicalNodePositionAware LRDP = new LRDP(
			new BytePacking(4));
	private static final String INDEX_DIR = "/opt/wiki-index";
	private IndexReader reader;

	public CorrectnessTest() throws IOException {
		Directory directory = MMapDirectory.open(new File(INDEX_DIR));
		reader = DirectoryReader.open(directory);
	}

	public static void main(String[] args) throws IOException {
		CorrectnessTest test = new CorrectnessTest();
		test.run();
	}

	private void run() throws IOException {
		Queries queries = new Queries();
		for (int i = 0; i < Queries.SIZE; i++) {
			TreeQuery query = queries.getQuery(i);
			Map<Integer, List<JoinType>> numDocsJoinType = new HashMap<Integer, List<JoinType>>();
			Map<Integer, List<JoinType>> numMatchesJoinType = new HashMap<Integer, List<JoinType>>();
			for (JoinType joinType : JoinType.values()) {
				if (query.hasBranches() && !joinType.allowsBranches()) {
					continue;
				}
				if (joinType.returnsFullResults()) {
					execute(joinType.getFullJoin(query, LRDP), reader,
							joinType, i, numDocsJoinType, numMatchesJoinType);
				} else {
					execute(joinType.getBooleanJoin(query, LRDP), reader,
							joinType, i, numDocsJoinType);
				}
			}
			if (numDocsJoinType.keySet().size() != 1) {
				System.err.println("Error in number of result docs for query " + i);
				System.err.println(numDocsJoinType.toString());
			}
			if (numMatchesJoinType.keySet().size() != 1) {
				System.err.println("Error in number of matches for query " + i);
				System.err.println(numMatchesJoinType.toString());
			}
		}
	}

	private void execute(ComputesBooleanResult join, IndexReader r, JoinType joinType,
			int queryId, Map<Integer, List<JoinType>> numDocsJoinType) throws IOException {
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
		if (!numDocsJoinType.containsKey(numResultDocs)) {
			numDocsJoinType.put(numResultDocs, new ArrayList<JoinType>());
		}
		numDocsJoinType.get(numResultDocs).add(joinType);
	}

	void execute(ComputesFullResults join, IndexReader r, JoinType joinType,
			int queryId, Map<Integer, List<JoinType>> numDocsJoinType, Map<Integer, List<JoinType>> numMatchesJoinType) throws IOException {
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
		if (!numDocsJoinType.containsKey(numResultDocs)) {
			numDocsJoinType.put(numResultDocs, new ArrayList<JoinType>());
		}
		numDocsJoinType.get(numResultDocs).add(joinType);
		if (!numMatchesJoinType.containsKey(totalMatches)) {
			numMatchesJoinType.put(totalMatches, new ArrayList<JoinType>());
		}
		numMatchesJoinType.get(totalMatches).add(joinType);
	}
}
