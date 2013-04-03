package au.edu.unimelb.csse.analyser;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;

import au.edu.unimelb.csse.paypack.LRDP;

public class TreeAnalyzerTest extends TestCase {
	private static final String FIELD = "content";
	private Directory d;
	private IndexReader r;

	@Before
	public void setUp() throws Exception {
		d = new RAMDirectory();
		Analyzer a = new TreeAnalyzer(new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111));
		IndexWriterConfig c = new IndexWriterConfig(Version.LUCENE_40, a);
		IndexWriter w = new IndexWriter(d, c);
		w.addDocument(getDoc("(AA BB)")); // doc 0
		w.addDocument(getDoc("(D(BB CC)(AA BB)(FFF G))")); // doc 1
		w.commit();
		w.close();
		r = DirectoryReader.open(d);
	}

	private Document getDoc(String text) {
		Document doc = new Document();
		doc.add(new TextField(FIELD, text, Store.NO));
		return doc;
	}

	@Test
	public void testTermAttributeValues() throws Exception {
		Term t = new Term(FIELD, "AA");
		int[] expFreq = new int[] { 1, 1 };
		int[][] expPos = new int[][] { new int[] { 0 }, new int[] { 3 } };
		int[][][] expPay = new int[][][] {
				new int[][] { new int[] { 0, 1, 0, 0 } },
				new int[][] { new int[] { 1, 2, 1, 4 } } };
		int numAtomicReaders = 0;
		assertFreqPosAndPayload(t, expFreq, expPos, expPay, numAtomicReaders);

		t = new Term(FIELD, "BB");
		expFreq = new int[] { 1, 2 };
		expPos = new int[][] { new int[] { 1 }, new int[] { 1, 4 } };
		expPay = new int[][][] {
				new int[][] { new int[] { 0, 1, 1, 1 } },
				new int[][] { new int[] { 0, 1, 1, 4 },
						new int[] { 1, 2, 2, 2 } } };
		assertFreqPosAndPayload(t, expFreq, expPos, expPay, numAtomicReaders);

	}

	private void assertFreqPosAndPayload(Term t, int[] expFreq, int[][] expPos,
			int[][][] expPay, int numAtomicReaders) throws IOException {
		List<AtomicReaderContext> leaves = r.getContext().leaves();
		for (AtomicReaderContext context : leaves) {
			AtomicReader reader = context.reader();
			DocsAndPositionsEnum termPositions = reader.termPositionsEnum(t);
			int docIndex = 0;
			while (termPositions.nextDoc() != DocsEnum.NO_MORE_DOCS) {
				assertEquals("Incorrect doc " + docIndex + " freq",
						expFreq[docIndex], termPositions.freq());
				assertEquals("Incorrect doc " + docIndex + " pos length",
						expPos[docIndex].length, termPositions.freq());
				int posIndex = 0;
				while (posIndex < termPositions.freq()) {
					int position = termPositions.nextPosition();
					assertEquals("Incorrect pos " + posIndex + " in doc "
							+ docIndex, expPos[docIndex][posIndex], position);
					BytesRef payload = termPositions.getPayload();
					int[] expPayload = expPay[docIndex][posIndex];
					String[] payloadDesc = new String[] { "left", "right",
							"depth", "parent" };
					for (int j = 0; j < 4; j++) {
						assertEquals(
								"Incorrect " + payloadDesc[j] + " payload",
								expPayload[j],
								payload.bytes[payload.offset + j]);
					}
					posIndex++;
				}
				docIndex++;
			}
			numAtomicReaders++;
		}
		assertEquals("Expected one atomic reader", 1, numAtomicReaders);
	}
}
