package au.edu.unimelb.csse.functional;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;

import au.edu.unimelb.csse.analyser.FastStringAnalyser;
import au.edu.unimelb.csse.queryParser.QueryBuilder;
import au.edu.unimelb.csse.search.SimpleHitCollector;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class StopAtFirstTest extends TestCase {
	public void testReturnsFalseIfThereAreNoMatchesWhenStopAtFirstIsTrue() throws Exception {
		Analyzer analyser = new FastStringAnalyser();
		RAMDirectory dir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(dir, analyser, true,
				IndexWriter.MaxFieldLength.UNLIMITED);
		String posMatch = "(NP" + "(NP" + "(DT The)" + "(NN year))" + "(NP"
				+ "(NP(CD 1956))" + "(PP" + "(IN in)"
				+ "(NP(JJ rugby)(NN union))" + ")" + ")" + "(. .)" + ")";
		Document d = new Document();
		d
				.add(new Field("sent", posMatch, Field.Store.NO,
						Field.Index.ANALYZED_NO_NORMS,
						Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);
		
		String negMatch = "(S(NP-SBJ(DT The)(NNP White)(NNP House))(VP(VBD said)(SBAR(-NONE- 0)(S(NP-SBJ-1(NNP Mr.)(NNP Bush))(VP(VP(VBD decided)(S(NP-SBJ(-NONE- *-1))(VP(TO to)(VP(VB grant)(NP(NP(JJ duty-free)(NN status))(PP(IN for)(NP(CD 18)(NNS categories))))))))(, ,)(CC but)(VP(VBD turned)(PRT(RP down))(NP(NP(JJ such)(NN treatment))(PP(IN for)(NP(NP(JJ other)(NNS types))(PP(IN of)(NP(NNS watches))))))(`` ``)(PP-PRD(RB because)(IN of)(NP(NP(DT the)(NN potential))(PP(IN for)(NP(NP(NN material)(NN injury))(PP(TO to)(NP(NP(VB watch)(NNS producers))(VP(VBN located)(NP(-NONE- *))(PP-LOC-CLR(IN in)(NP(NP(DT the)(NNP U.S.))(CC and)(NP(DT the)(NNP Virgin)(NNPS Islands))))))))))))))))(. .)('' ''))";
		d = new Document();
		d
				.add(new Field("sent", negMatch, Field.Store.NO,
						Field.Index.ANALYZED_NO_NORMS,
						Field.TermVector.WITH_POSITIONS));
		writer.addDocument(d);

		writer.close();

		IndexSearcher searcher = new IndexSearcher(dir);
		QueryBuilder builder = new QueryBuilder("/NP");
		TreebankQuery query = builder.parse(TermJoinType.EARLY_STOP_WITH_FC, true);
		SimpleHitCollector hitCollector = new SimpleHitCollector(10);
		searcher.search(query, hitCollector);
		assertEquals(1, hitCollector.totalHits);
	}
}
