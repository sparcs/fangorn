package au.edu.unimelb.csse.exp;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import au.edu.unimelb.csse.CountingOperatorAware;
import au.edu.unimelb.csse.join.ComputesBooleanResult;
import au.edu.unimelb.csse.join.ComputesFullResults;
import au.edu.unimelb.csse.paypack.LRDP;
import au.edu.unimelb.csse.paypack.LogicalNodePositionDecorator;

public class NumComparisonsTest {

	private DirectoryReader reader;

	public NumComparisonsTest(String indexDir) throws IOException {
		Directory directory = MMapDirectory.open(new File(indexDir));
		reader = DirectoryReader.open(directory);
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		NumComparisonsTest test = new NumComparisonsTest(args[0]);
		test.run();
	}

	private void run() throws IOException {
		JoinType[] joinTypes = JoinType.values();
		String headerline = "-";
		for (JoinType joinType : joinTypes) {
			headerline += "," + joinType.name();
		}
		System.out.println(headerline);
		String line;
		Set<JoinType> supportsHorizOprs = new HashSet<JoinType>();
		supportsHorizOprs.add(JoinType.BASELINE1);
		supportsHorizOprs.add(JoinType.BASELINE2);
		supportsHorizOprs.add(JoinType.STAIRCASE);
		supportsHorizOprs.add(JoinType.LATE);
		supportsHorizOprs.add(JoinType.LATEMRR);
		Queries queries = new Queries();
		for (int q = 0; q < 45; q++) {
			line = String.valueOf(q);
			for (JoinType joinType : joinTypes) {
				TreeQuery query = queries.getQuery(q);
				if ((query.hasBranches() && !joinType.allowsBranches())
						|| (q > 35 && !supportsHorizOprs.contains(joinType))) {
					line += ",";
				} else {
					LogicalNodePositionDecorator lrdp = new LogicalNodePositionDecorator(new LRDP(
							LRDP.PhysicalPayloadFormat.BYTE1111));
					CountingOperatorAware countingOperatorAware = lrdp.getCountingOperatorAware();
					if (joinType.returnsFullResults()) {
						execute(joinType.getFullJoin(query, lrdp), reader,
								joinType, q);
					} else {
						execute(joinType.getBooleanJoin(query, lrdp), reader,
								joinType, q);
					}
					line += "," + countingOperatorAware.getCount();
				}
			}
			System.out.println(line);
		}
	}

	private void execute(ComputesBooleanResult join, IndexReader r,
			JoinType joinType, int queryId) throws IOException {
		int docId = -1;
		join.setup(r);
		docId = join.nextDoc();
		while (docId != DocIdSetIterator.NO_MORE_DOCS) {
			join.match();
			docId = join.nextDoc();
		}
	}

	private void execute(ComputesFullResults join, IndexReader r,
			JoinType joinType, int queryId) throws IOException {
		int docId = -1;
		join.setup(r);
		docId = join.nextDoc();
		while (docId != DocIdSetIterator.NO_MORE_DOCS) {
			join.match();
			docId = join.nextDoc();
		}
	}

}
