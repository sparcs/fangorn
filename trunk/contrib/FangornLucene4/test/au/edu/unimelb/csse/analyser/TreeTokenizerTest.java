package au.edu.unimelb.csse.analyser;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import au.edu.unimelb.csse.paypack.LRDP;

public class TreeTokenizerTest extends TestCase {
	private TreeTokenizer tokenizer = new TreeTokenizer(new StringReader(""),
			new LRDP(LRDP.PhysicalPayloadFormat.BYTE1111));

	@Test
	public void testUnreal2TermSentence() throws Exception {
		resetTokenizer("(AA BB)");

		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
	}

	@Test
	public void testUnreal2TermSentenceWithSpaces() throws Exception {
		resetTokenizer("( AA BB)");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);

		resetTokenizer("(AA  BB)");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);

		resetTokenizer("(AA BB )");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);

		resetTokenizer("(AA BB) ");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);

		resetTokenizer(" ( AA   BB ) ");
		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 1);
	}

	@Test
	public void test1ChildRootTree() throws Exception {
		resetTokenizer("(AA(BB CC))");

		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 2);
		assertNextToken("CC", 0, 1, 2, 1);
	}

	@Test
	public void test1ChildRootTreeWithSpaces() throws Exception {
		resetTokenizer(" ( AA (  BB   CC ) )  ");

		assertNextToken("AA", 0, 1, 0, 0);
		assertNextToken("BB", 0, 1, 1, 2);
		assertNextToken("CC", 0, 1, 2, 1);
	}

	@Test
	public void test2ChildrenRootTree() throws Exception {
		resetTokenizer("(AA(BB CC)(D EEE))");

		assertNextToken("AA", 0, 2, 0, 0);
		assertNextToken("BB", 0, 1, 1, 3);
		assertNextToken("CC", 0, 1, 2, 1);
		assertNextToken("D", 1, 2, 1, 3);
		assertNextToken("EEE", 1, 2, 2, 2);
	}

	@Test
	public void test3ChildrenRootTree() throws Exception {
		resetTokenizer("(AA(BB CC)(D EEE)(FFF G))");

		assertNextToken("AA", 0, 3, 0, 0);
		assertNextToken("BB", 0, 1, 1, 4);
		assertNextToken("CC", 0, 1, 2, 1);
		assertNextToken("D", 1, 2, 1, 4);
		assertNextToken("EEE", 1, 2, 2, 2);
		assertNextToken("FFF", 2, 3, 1, 4);
		assertNextToken("G", 2, 3, 2, 3);
	}

	@Test
	public void testNestedTree() throws Exception {
		resetTokenizer("(A(B(C D)(E F))(G(H(I J))(K L)))");

		assertNextToken("A", 0, 4, 0, 0);
		assertNextToken("B", 0, 2, 1, 8);
		assertNextToken("C", 0, 1, 2, 3);
		assertNextToken("D", 0, 1, 3, 1);
		assertNextToken("E", 1, 2, 2, 3);
		assertNextToken("F", 1, 2, 3, 2);
		assertNextToken("G", 2, 4, 1, 8);
		assertNextToken("H", 2, 3, 2, 7);
		assertNextToken("I", 2, 3, 3, 5);
		assertNextToken("J", 2, 3, 4, 4);
		assertNextToken("K", 3, 4, 2, 7);
		assertNextToken("L", 3, 4, 3, 6);
	}

	@Test
	public void testNestedTreeWithSpace() throws Exception {
		resetTokenizer(" (  A ( B ( C D ) ( E F ) )  ( G ( H ( I  J ) ) ( K  L ) ) ) ");

		assertNextToken("A", 0, 4, 0, 0);
		assertNextToken("B", 0, 2, 1, 8);
		assertNextToken("C", 0, 1, 2, 3);
		assertNextToken("D", 0, 1, 3, 1);
		assertNextToken("E", 1, 2, 2, 3);
		assertNextToken("F", 1, 2, 3, 2);
		assertNextToken("G", 2, 4, 1, 8);
		assertNextToken("H", 2, 3, 2, 7);
		assertNextToken("I", 2, 3, 3, 5);
		assertNextToken("J", 2, 3, 4, 4);
		assertNextToken("K", 3, 4, 2, 7);
		assertNextToken("L", 3, 4, 3, 6);
	}
	
	public void testSentence() throws Exception {
		resetTokenizer("(S1 (S (NP (NNP Beatrice)) (VP (AUX is) (NP (DT a) (JJ Hungarian) (NN rock) (NN band))) (. .)))");
		
		assertNextToken("S1", 0, 7, 0, 0);
		assertNextToken("S", 0, 7, 1, 12);
		assertNextToken("NP", 0, 1, 2, 11);
		assertNextToken("NNP", 0, 1, 3, 2);
		assertNextToken("Beatrice", 0, 1, 4, 1);
		assertNextToken("VP", 1, 6, 2, 11);
		assertNextToken("AUX", 1, 2, 3, 9);
		assertNextToken("is", 1, 2, 4, 3);
		assertNextToken("NP", 2, 6, 3, 9);
		assertNextToken("DT", 2, 3, 4, 8);
		assertNextToken("a", 2, 3, 5, 4);
		assertNextToken("JJ", 3, 4, 4, 8);
		assertNextToken("Hungarian", 3, 4, 5, 5);
		assertNextToken("NN", 4, 5, 4, 8);
		assertNextToken("rock", 4, 5, 5, 6);
		assertNextToken("NN", 5, 6, 4, 8);
		assertNextToken("band", 5, 6, 5, 7);
		assertNextToken(".", 6, 7, 2, 11);
		assertNextToken(".", 6, 7, 3, 10);
	}

	@Test
	public void testIncorrectSentencesAreIdentified() throws Exception {
		assertThrowsError("(A)");
		assertThrowsError("(A )");
		assertThrowsError("(A B");
		assertThrowsError("(A(B(C D))");
		assertThrowsError("(A B C)");
		assertThrowsError("(A (B C D))");
		assertThrowsError("(A (B C)))");
		assertThrowsError("A");
	}

	private void assertNextToken(final String string, final int left,
			final int right, final int depth, final int parent)
			throws IOException {
		boolean success = tokenizer.incrementToken();
		assertTrue(success);
		assertText(string);
		assertPosition();
		assertPayload(left, right, depth, parent);
	}

	private void assertPosition() {
		PositionIncrementAttribute posAttribute = tokenizer
				.getAttribute(PositionIncrementAttribute.class);
		assertEquals("Incorrect token position increment", 1,
				posAttribute.getPositionIncrement());
	}

	private void assertText(final String string) {
		CharTermAttribute charText = tokenizer
				.getAttribute(CharTermAttribute.class);
		assertEquals(string, charText.toString());
	}

	private void assertThrowsError(final String s) {
		try {
			resetTokenizer(s);
			fail();
		} catch (Exception e) {

		}
	}

	private void resetTokenizer(String s) throws IOException {
		tokenizer.setReader(new StringReader(s));
		tokenizer.reset();
	}

	private void assertPayload(int left, int right, int depth, int parent) {
		PayloadAttribute payloadAttribute = tokenizer
				.getAttribute(PayloadAttribute.class);
		BytesRef payload = payloadAttribute.getPayload();
		assertEquals("Incorrect left payload", left,
				payload.bytes[payload.offset + 0]);
		assertEquals("Incorrect right payload", right,
				payload.bytes[payload.offset + 1]);
		assertEquals("Incorrect depth payload", depth,
				payload.bytes[payload.offset + 2]);
		assertEquals("Incorrect parent payload", parent,
				payload.bytes[payload.offset + 3]);
	}
}
