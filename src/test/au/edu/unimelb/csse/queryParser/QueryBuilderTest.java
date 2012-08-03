/*******************************************************************************
 * Copyright 2011 The fangorn project
 * 
 *        Author: Sumukh Ghodke
 * 
 *        Licensed to the Apache Software Foundation (ASF) under one
 *        or more contributor license agreements.  See the NOTICE file
 *        distributed with this work for additional information
 *        regarding copyright ownership.  The ASF licenses this file
 *        to you under the Apache License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance
 *        with the License.  You may obtain a copy of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *        Unless required by applicable law or agreed to in writing,
 *        software distributed under the License is distributed on an
 *        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *        KIND, either express or implied.  See the License for the
 *        specific language governing permissions and limitations
 *        under the License.
 ******************************************************************************/
package au.edu.unimelb.csse.queryParser;

import java.nio.charset.Charset;
import java.util.List;

import junit.framework.TestCase;
import au.edu.unimelb.csse.ParseException;
import au.edu.unimelb.csse.axis.TreeAxis;
import au.edu.unimelb.csse.search.FilterChunk;
import au.edu.unimelb.csse.search.TermFilter;
import au.edu.unimelb.csse.search.TreeExpr;
import au.edu.unimelb.csse.search.TreeTerm;
import au.edu.unimelb.csse.search.TreebankQuery;
import au.edu.unimelb.csse.search.join.TermJoinType;

public class QueryBuilderTest extends TestCase {
	public void testIdentifiesSingleOperatorAndTerm() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP");
		final TreeExpr expr = builder.getExpr();
		assertNotNull(expr);
		assertEquals(1, expr.size());
		assertEquals(TreeAxis.DESCENDANT, expr.getTerm(0).axis());
		assertEquals("NP", expr.getTerm(0).termLabel());
		assertFalse(expr.getTerm(0).hasFilter());
	}

	public void testIdentifiesTwoOperatorsAndTerms() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP//VP");
		final TreeExpr expr = builder.getExpr();
		assertNotNull(expr);
		assertEquals(2, expr.size());
		assertEquals(TreeAxis.DESCENDANT, expr.getTerm(0).axis());
		assertEquals("NP", expr.getTerm(0).termLabel());
		assertFalse(expr.getTerm(0).hasFilter());
		assertEquals(TreeAxis.DESCENDANT, expr.getTerm(1).axis());
		assertEquals("VP", expr.getTerm(1).termLabel());
		assertFalse(expr.getTerm(1).hasFilter());
	}

	public void testSimpleFilterExpression() throws Exception {
		QueryBuilder builder = new QueryBuilder("//VP[//NP]");
		final TreeExpr expr = builder.getExpr();
		assertNotNull(expr);
		assertEquals("//VP[//NP]", expr.toString());
	}

	public void testFilterExpressionWithOperators() throws Exception {
		QueryBuilder builder = new QueryBuilder(
				"//AA[//BB OR //CC & !\\DD OR ==>EE]");
		assertEquals(16, builder.tokens().size());
		final TreeExpr expr = builder.getExpr();
		assertNotNull(expr);
		assertEquals("//AA[//BB | //CC & !\\DD | ==>EE]", expr.toString());
	}

	public void testTokenization() throws Exception {
		QueryBuilder builder = new QueryBuilder("// NP");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("NP", tokens.get(1));
	}

	public void testTokenizingTreeExpr() throws Exception {
		QueryBuilder builder = new QueryBuilder("//VP[//NP]");
		List<String> tokens = builder.tokens();
		assertEquals(6, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("VP", tokens.get(1));
	}

	public void testFilterExpressionToken() throws Exception {
		QueryBuilder builder = new QueryBuilder("//VP[//NP]");
		List<String> tokens = builder.tokens();
		assertEquals(6, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("VP", tokens.get(1));
		assertEquals("[", tokens.get(2));
		assertEquals("//", tokens.get(3));
		assertEquals("NP", tokens.get(4));
		assertEquals("]", tokens.get(5));
	}

	public void testTokenizingAncestorSymbol() throws Exception {
		QueryBuilder builder = new QueryBuilder("//VP\\NP");
		List<String> tokens = builder.tokens();
		assertEquals(4, tokens.size());
		assertEquals("\\", tokens.get(2));

		builder = new QueryBuilder("//VP\\\\NP");
		tokens = builder.tokens();
		assertEquals(4, tokens.size());
		assertEquals("\\\\", tokens.get(2));

	}

	public void testTokenize_None_() throws Exception {
		QueryBuilder builder = new QueryBuilder("//-NONE-");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("-NONE-", tokens.get(1));
	}

	public void testTokenizeComma() throws Exception {
		QueryBuilder builder = new QueryBuilder("//,");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals(",", tokens.get(1));
	}

	public void testTokenizeFullStop() throws Exception {
		QueryBuilder builder = new QueryBuilder("// .");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals(".", tokens.get(1));
	}

	public void testTokenizeStar() throws Exception {
		QueryBuilder builder = new QueryBuilder("// *");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("*", tokens.get(1));
	}

	public void testTokenizeSpecialSymbols() throws Exception {
		QueryBuilder builder = new QueryBuilder("//*T*-1");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("*T*-1", tokens.get(1));

		builder = new QueryBuilder("//*-1");
		tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("*-1", tokens.get(1));
	}

	public void testNumberOfTokenswithFollowingOp() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP//NP//NP-->NP");
		List<String> tokens = builder.tokens();
		assertEquals(8, tokens.size());
	}

	public void testTokenizeDoubleFilter() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP//VP[//PP-LOC[/IN]]");
		List<String> tokens = builder.tokens();
		assertEquals(12, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("NP", tokens.get(1));
		assertEquals("//", tokens.get(2));
		assertEquals("VP", tokens.get(3));
		assertEquals("[", tokens.get(4));
		assertEquals("//", tokens.get(5));
		assertEquals("PP-LOC", tokens.get(6));
		assertEquals("[", tokens.get(7));
		assertEquals("/", tokens.get(8));
		assertEquals("IN", tokens.get(9));
		assertEquals("]", tokens.get(10));
		assertEquals("]", tokens.get(11));
	}

	public void testValidateDoubleFilter() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP[//PP-LOC[/IN]]");
		try {
			builder.tokens();
		} catch (Exception pe) {
			fail(pe.getMessage());
		}
	}

	public void testDoubleFilterExpr() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP[//PP-LOC[/IN]]");
		try {
			TreeExpr expr = builder.getExpr();
			TreeTerm term = expr.getTerm(0);
			assertEquals(TreeAxis.DESCENDANT, term.axis());
			assertEquals("NP", term.termLabel());
			assertTrue(term.hasFilter());
			TermFilter filter = term.filter();
			FilterChunk[] chunks = filter.chunks();
			assertEquals(1, chunks.length);
			assertEquals(0, chunks[0].notElements().length);
			assertEquals(1, chunks[0].elements().length);
			TreeTerm pploc = (TreeTerm) chunks[0].elements()[0];
			assertEquals(TreeAxis.DESCENDANT, (pploc).axis());
			assertEquals("PP-LOC", pploc.termLabel());
			assertTrue(pploc.hasFilter());
			TermFilter pplocfilter = pploc.filter();
			FilterChunk[] pplocchunks = pplocfilter.chunks();
			assertEquals(1, pplocchunks.length);
			assertEquals(0, pplocchunks[0].notElements().length);
			assertEquals(1, pplocchunks[0].elements().length);
			TreeTerm in = (TreeTerm) pplocchunks[0].elements()[0];
			assertEquals(TreeAxis.CHILD, in.axis());
			assertEquals("IN", in.termLabel());
		} catch (ParseException pe) {
			fail(pe.getMessage());
		}
	}

	public void testTokenizeNestedFilter2() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP[//PP-LOC[/IN]//PP=>NP]");
		List<String> tokens = builder.tokens();
		assertEquals(14, tokens.size());
	}

	public void testValidateNestedFilter2() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP[//PP-LOC[/IN]//PP=>NP]");
		try {
			builder.tokens();
		} catch (Exception pe) {
			fail(pe.getMessage());
		}
	}

	public void testNestedFilterExpr2() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP[//PP-LOC[/IN]//PP=>NP]");
		try {
			TreeExpr expr = builder.getExpr();
			TreeTerm term = expr.getTerm(0);
			assertEquals(TreeAxis.DESCENDANT, term.axis());
			assertEquals("NP", term.termLabel());
			assertTrue(term.hasFilter());
			TermFilter filter = term.filter();
			FilterChunk[] chunks = filter.chunks();
			assertEquals(1, chunks.length);
			assertEquals(0, chunks[0].notElements().length);
			assertEquals(1, chunks[0].elements().length);
			TreeExpr pplocExpr = (TreeExpr) chunks[0].elements()[0];
			assertEquals(3, pplocExpr.size());
			TreeTerm pploc = pplocExpr.getTerm(0);
			assertEquals(TreeAxis.DESCENDANT, (pploc).axis());
			assertEquals("PP-LOC", pploc.termLabel());
			assertTrue(pploc.hasFilter());
			TermFilter pplocfilter = pploc.filter();
			FilterChunk[] pplocchunks = pplocfilter.chunks();
			assertEquals(1, pplocchunks.length);
			assertEquals(0, pplocchunks[0].notElements().length);
			assertEquals(1, pplocchunks[0].elements().length);
			TreeTerm in = (TreeTerm) pplocchunks[0].elements()[0];
			assertEquals(TreeAxis.CHILD, in.axis());
			assertEquals("IN", in.termLabel());
			TreeTerm pp = pplocExpr.getTerm(1);
			assertEquals(TreeAxis.DESCENDANT, pp.axis());
			assertEquals("PP", pp.termLabel());
			TreeTerm np = pplocExpr.getTerm(2);
			assertEquals(TreeAxis.IMMEDIATE_FOLLOWING_SIBLING, np.axis());
			assertEquals("NP", np.termLabel());
		} catch (ParseException pe) {
			fail(pe.getMessage());
		}
	}

	public void testDoubleDashes() {
		QueryBuilder builder = new QueryBuilder("//NP-SBJ-1");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("NP-SBJ-1", tokens.get(1));
	}

	public void testMultipleDashes() throws Exception {
		QueryBuilder builder = new QueryBuilder("//PP-LOC-PRD");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("PP-LOC-PRD", tokens.get(1));

		builder = new QueryBuilder("//PP-LOC-PRD-TPC-3");
		tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("PP-LOC-PRD-TPC-3", tokens.get(1));
	}

	public void testDollarAtTheEnd() throws Exception {
		QueryBuilder builder = new QueryBuilder("//PRP$");
		List<String> tokens = builder.tokens();
		assertEquals(2, tokens.size());
		assertEquals("//", tokens.get(0));
		assertEquals("PRP$", tokens.get(1));
	}

	public void testFilterWithLogicalAndAndNested() throws ParseException {
		QueryBuilder builder = new QueryBuilder(
				"//NP[//NP[//DT->JJ->NN] and //PP/IN/for]");
		List<String> tokens = builder.tokens();

		assertEquals(21, tokens.size());
		TreeExpr expr = builder.getExpr();
		assertEquals("NP", expr.getTerm(0).termLabel());
		assertEquals("//NP[//DT->JJ->NN]",
				(expr.getTerm(0).filter().chunks()[0].elements()[0]).toString());
	}

	public void testParses() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NORML<-VFL");
		final List<String> tokens = builder.tokens();
		assertEquals(4, tokens.size());
		
		TreebankQuery tq = builder.parse(TermJoinType.EARLY_STOP_WITH_FC, false);
		assertNotNull(tq);
	}
	
	public void test3Dashes() throws Exception {
		QueryBuilder builder = new QueryBuilder("//--->ESPN-->VP");
		List<String> tokens = builder.tokens();
		assertEquals(6, tokens.size());
		assertEquals("-", tokens.get(1));
		assertEquals("-->", tokens.get(2));
		
		builder = new QueryBuilder("//-- ->ESPN-->VP");
		tokens = builder.tokens();
		assertEquals(6, tokens.size());
		assertEquals("--", tokens.get(1));
		assertEquals("->", tokens.get(2));

	}

	// public void testVerify() throws Exception {
	// assertTokenizationDoesNotThrowException("//VP[//NP]");
	// assertTokenizationDoesNotThrowException("//VP[//NP and //VP]");
	// assertTokenizationDoesNotThrowException("//VP[//NP & //VP]");
	// assertTokenizationDoesNotThrowException("//VP[//NP & !//VP]");
	// assertTokenizationDoesNotThrowException("//VP[//NP & not //VP]");
	// assertTokenizationDoesNotThrowException("//VP[//NP and not //VP]");
	// assertTokenizationDoesNotThrowException("//NP//NP//NP/NP");
	// assertTokenizationDoesNotThrowException("//NP//PP-LOC");
	// assertTokenizationDoesNotThrowException("//NP//NP//NP-->NP");
	// assertTokenizationDoesNotThrowException("//NP[not//VP]");
	// assertTokenizationDoesNotThrowException("\\\\NP");
	//
	// // here multiple dashes are considered a label
	// assertTokenizationDoesNotThrowException("//NP//---- -->NP[//NP]");
	//
	// assertTokenizationThrowsException("//NP//NP//NP[NP]");
	// assertTokenizationThrowsException("//NP//NP//NP[//NP");
	// // operators only allowed within filter expressions
	// assertTokenizationThrowsException("//NP and //NP");
	// assertTokenizationThrowsException("//NP//NP//NP[//NP and ]");
	// assertTokenizationThrowsException("//NP//NP//NP[//NP not ]");
	// assertTokenizationThrowsException("//NP//NP//NP[//NP not and //JJ]");
	// // double dash not allowed
	// assertTokenizationThrowsException("//NP//NP--NP[//NP]");
	//
	// }
	//
	// private void assertTokenizationDoesNotThrowException(String query) {
	// QueryBuilder3 builder = new QueryBuilder3(query);
	// try {
	// builder.tokens();
	// } catch (Exception e) {
	// fail();
	// }
	// }
	//
	// private void assertTokenizationThrowsException(String query) {
	// QueryBuilder3 builder = new QueryBuilder3(query);
	// try {
	// builder.tokens();
	// fail();
	// } catch (Exception e) {
	// }
	// }

	public void testReturnsCompleteQuery() throws Exception {
		QueryBuilder builder = new QueryBuilder("//NP//NP");
		TreebankQuery query = builder.parse(TermJoinType.EARLY_STOP_WITH_FC,
				false);
		assertNotNull(query);
	}

	public void testFirstOperatorIsChild() throws Exception {
		QueryBuilder builder = new QueryBuilder("/NP//NP");
		TreebankQuery query = builder.parse(TermJoinType.EARLY_STOP_WITH_FC,
				false);
		assertNotNull(query);
		assertEquals("/NP//NP", query.getTreeExpr().toString());
	}

	public void testPreceedingSibling() throws Exception {
		QueryBuilder builder = new QueryBuilder("//VP<=NP");
		TreebankQuery query = builder.parse(TermJoinType.EARLY_STOP_WITH_FC,
				false);
		assertNotNull(query);
		assertEquals("//VP<=NP", query.getTreeExpr().toString());
	}

	public void testApostrophieInLabel() throws Exception {
		QueryBuilder builder = new QueryBuilder("//n't");
		TreebankQuery query = builder.parse(TermJoinType.EARLY_STOP_WITH_FC,
				false);
		assertNotNull(query);
		assertEquals("//n't", query.getTreeExpr().toString());
	}

	public void testWhackyLabel() throws Exception {
		QueryBuilder builder = new QueryBuilder("//Magnificient\"-RRB-.");
		TreebankQuery query = builder.parse(TermJoinType.EARLY_STOP_WITH_FC,
				false);
		assertNotNull(query);
		assertEquals("//Magnificient\"-RRB-.", query.getTreeExpr().toString());
	}
	
	public void testFilterIdsAreInIncreasingOrder() throws Exception {
		QueryBuilder builder = new QueryBuilder(
				"//AA[//BB OR //CC & !\\DD OR ==>EE]");
		TreebankQuery query = builder.parse(TermJoinType.SIMPLE, false);
		TreeExpr treeExpr = query.getTreeExpr();
		TreeTerm aaTerm = treeExpr.getTerm(0);
		assertEquals("AA", aaTerm.termLabel());
		assertEquals(0, aaTerm.getId());
		TreeTerm bbTerm = (TreeTerm) aaTerm.filter().chunks()[0].elements()[0];
		assertEquals("BB", bbTerm.termLabel());
		assertEquals(1, bbTerm.getId());
		TreeTerm ccTerm = (TreeTerm) aaTerm.filter().chunks()[1].elements()[0];
		assertEquals("CC", ccTerm.termLabel());
		assertEquals(2, ccTerm.getId());
		TreeTerm ddTerm = (TreeTerm) aaTerm.filter().chunks()[1].notElements()[0];
		assertEquals("DD", ddTerm.termLabel());
		assertEquals(3, ddTerm.getId());
		TreeTerm eeTerm = (TreeTerm) aaTerm.filter().chunks()[2].elements()[0];
		assertEquals("EE", eeTerm.termLabel());
		assertEquals(4, eeTerm.getId());
		
		builder = new QueryBuilder("//NP[//NP-SBJ[//DT->JJ->NN] and //PP/IN/for]");
		query = builder.parse(TermJoinType.SIMPLE, false);
		TreeExpr mainExpr = query.getTreeExpr();
		TreeTerm np = mainExpr.getTerm(0);
		assertEquals("NP", np.termLabel());
		assertEquals(0, np.getId());
		TermFilter f = np.filter();
		assertEquals(1, f.chunks().length);
		TreeTerm npsbj = (TreeTerm) f.chunks()[0].elements()[0];
		assertEquals("NP-SBJ", npsbj.termLabel());
		assertEquals(1, npsbj.getId());
		TreeExpr npsbjfexpr = (TreeExpr) npsbj.filter().chunks()[0].elements()[0];
		TreeTerm dt = npsbjfexpr.getTerm(0);
		assertEquals("DT", dt.termLabel());
		assertEquals(2, dt.getId());
		TreeTerm jj = npsbjfexpr.getTerm(1);
		assertEquals("JJ", jj.termLabel());
		assertEquals(3, jj.getId());
		TreeTerm nn = npsbjfexpr.getTerm(2);
		assertEquals("NN", nn.termLabel());
		assertEquals(4, nn.getId());
		TreeExpr exp3 = (TreeExpr) np.filter().chunks()[0].elements()[1];
		TreeTerm pp = exp3.getTerm(0);
		assertEquals("PP", pp.termLabel());
		assertEquals(5, pp.getId());
		TreeTerm interm = exp3.getTerm(1);
		assertEquals("IN", interm.termLabel());
		assertEquals(6, interm.getId());
		TreeTerm wordFor = exp3.getTerm(2);
		assertEquals("for", wordFor.termLabel());
		assertEquals(7, wordFor.getId());
	}

	public void testNotFilterIdsAreInIncreasingOrder() throws Exception {
		QueryBuilder builder = new QueryBuilder("//AA[//BB OR !//CC OR \\DD OR !==>EE]");
		TreebankQuery query = builder.parse(TermJoinType.SIMPLE, false);
		TreeExpr treeExpr = query.getTreeExpr();
		TreeTerm aa = treeExpr.getTerm(0);
		assertEquals("AA", aa.termLabel());
		assertEquals(0, aa.getId());
		TreeTerm bb = (TreeTerm) aa.filter().chunks()[0].elements()[0];
		assertEquals("BB", bb.termLabel());
		assertEquals(1,bb.getId());
		TreeTerm cc = (TreeTerm) aa.filter().chunks()[1].notElements()[0];
		assertEquals("CC", cc.termLabel());
		assertEquals(2,cc.getId());
		TreeTerm dd = (TreeTerm) aa.filter().chunks()[2].elements()[0];
		assertEquals("DD", dd.termLabel());
		assertEquals(3,dd.getId());
		TreeTerm ee = (TreeTerm) aa.filter().chunks()[3].notElements()[0];
		assertEquals("EE", ee.termLabel());
		assertEquals(4,ee.getId());

		builder = new QueryBuilder("//AA[//BB AND !//CC AND \\DD AND !==>EE]");
		query = builder.parse(TermJoinType.SIMPLE, false);
		treeExpr = query.getTreeExpr();
		aa = treeExpr.getTerm(0);
		assertEquals("AA", aa.termLabel());
		assertEquals(0, aa.getId());
		bb = (TreeTerm) aa.filter().chunks()[0].elements()[0];
		assertEquals("BB", bb.termLabel());
		assertEquals(1,bb.getId());
		cc = (TreeTerm) aa.filter().chunks()[0].notElements()[0];
		assertEquals("CC", cc.termLabel());
		assertEquals(2,cc.getId());
		dd = (TreeTerm) aa.filter().chunks()[0].elements()[1];
		assertEquals("DD", dd.termLabel());
		assertEquals(3,dd.getId());
		ee = (TreeTerm) aa.filter().chunks()[0].notElements()[1];
		assertEquals("EE", ee.termLabel());
		assertEquals(4,ee.getId());

	}
}
