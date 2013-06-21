package au.edu.unimelb.csse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
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
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;

import au.edu.unimelb.csse.analyser.TreeAnalyzer;
import au.edu.unimelb.csse.join.NodePositions;
import au.edu.unimelb.csse.paypack.LRDP;

public abstract class IndexTestCase extends TestCase {
	protected static final FieldType fieldType = getFieldType();
	protected Directory d;

	@Override
	@Before
	protected void setUp() throws Exception {
		super.setUp();
		d = new RAMDirectory();
	}

	@Override
	@After
	protected void tearDown() throws Exception {
		super.tearDown();
		d.close();
	}

	protected IndexReader commitIndexAndOpenReader(IndexWriter w)
			throws IOException {
		w.commit();
		w.close();
		IndexReader r = DirectoryReader.open(d);
		return r;
	}

	protected IndexWriter setupIndex() throws IOException {
		Analyzer a = new TreeAnalyzer(new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111));
		IndexWriterConfig c = new IndexWriterConfig(Version.LUCENE_42, a);
		IndexWriter w = new IndexWriter(d, c);
		return w;
	}

	protected IndexReader setupIndexWithDocs(String... docs) throws IOException {
		IndexWriter w = setupIndex();
		for (String doc : docs) {
			w.addDocument(getDoc(doc));
		}
		return commitIndexAndOpenReader(w);
	}

	protected DocsAndPositionsEnum getPosEnum(IndexReader r, int docid, Term t)
			throws IOException {
		List<AtomicReaderContext> leaves = r.getContext().leaves();
		for (AtomicReaderContext context : leaves) {
			AtomicReader reader = context.reader();
			DocsAndPositionsEnum termPositions = reader.termPositionsEnum(t);
			int doc;
			while ((doc = termPositions.nextDoc()) != DocsEnum.NO_MORE_DOCS
					&& doc != docid) {
			}
			if (doc != DocsEnum.NO_MORE_DOCS) {
				return termPositions;
			}
		}
		assertFalse("Expected positions enum for doc " + docid, true);
		return null; // will never come here
	}

	private static FieldType getFieldType() {
		FieldType ft = new FieldType();
		ft.setIndexed(true);
		ft.setTokenized(true);
		ft.setOmitNorms(true);
		ft.freeze();
		return ft;
	}

	protected Document getDoc(String string) {
		Document d = new Document();
		d.add(new Field("s", string, fieldType));
		return d;
	}

	protected void assertIntArray(int[] expected, int[] returned) {
		assertEquals("Returned array: " + Arrays.toString(returned)
				+ "; Array lengths are unequal", expected.length,
				returned.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Incorrect element at pos " + i + ".", expected[i],
					returned[i]);
		}
	}

	protected void assertPositions(int[] expected, int expectedOffset,
			NodePositions prev) {
		assertEquals("Incorrect number of positions", expected.length,
				prev.size);
		assertEquals("Incorrect offset", expectedOffset, prev.offset);
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Incorrect value at index " + i, expected[i],
					prev.positions[i]);
		}
	}
	
	protected void assertPositions(String message, int[] expected, int expectedOffset,
			NodePositions prev) {
		assertEquals(message + " Incorrect number of positions", expected.length,
				prev.size);
		assertEquals(message + " Incorrect offset", expectedOffset, prev.offset);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(message + " Incorrect value at index " + i, expected[i],
					prev.positions[i]);
		}
	}
}
