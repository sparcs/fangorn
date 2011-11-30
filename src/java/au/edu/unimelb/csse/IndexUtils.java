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
package au.edu.unimelb.csse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

public class IndexUtils {
	private IndexReader reader;

	private static final String TOTAL_NUM_TERMS = "total_num_terms";

	private static final String TERMS_BY_DOC_FREQ = "terms_by_doc_freq";

	private static final String MAX_TERM_FREQ = "max_term_freq";

	public IndexUtils(String indexPath) throws CorruptIndexException,
			IOException {
		reader = IndexReader.open(indexPath);
	}

	public Map<String, Object> getTotalTermsAndTermsByDocFreq()
			throws IOException {
		Map<String, Object> r = new HashMap<String, Object>();
		TermEnum terms = reader.terms();
		int max = 0;
		int num = 0;
		Map<Integer, List<String>> counts = new HashMap<Integer, List<String>>();
		while (terms.next()) {
			int docFreq = terms.docFreq();
			if (docFreq > max) {
				max = docFreq;
			}
			if (!counts.containsKey(docFreq)) {
				counts.put(docFreq, new ArrayList<String>());
			}
			counts.get(docFreq).add(terms.term().text());
			num++;
		}
		r.put(TOTAL_NUM_TERMS, num);
		r.put(TERMS_BY_DOC_FREQ, counts);
		r.put(MAX_TERM_FREQ, max);
		return r;
	}

	public void writeCountsToFile(int[] lengths) throws IOException {
		Writer writer = new FileWriter(new File("counts"));
		int sum = 0;
		for (int i = 0; i < lengths.length;) {
			if (i == 0) {
				sum += lengths[0];
				writer.write(String.valueOf(i + 1) + "\t" + Math.log10(sum)
						+ "\n");
				writer.flush();
				i++;
				continue;
			}
			int exp = 0;
			for (int pow = 1; pow < 12; pow++) {
				if (i + 1 <= ((int) Math.pow(10, pow))) {
					exp = pow;
					break;
				}
			}
			int start = i;
			int rpow = ((int) Math.pow(10, exp - 1));
			for (int k = 0; k < 9; k++) {
				if (!((start + (k * rpow)) < lengths.length)) {
					break;
				}
				sum += lengths[start + (k * rpow)];
				i++;
				for (int l = 1; l < rpow; l++) {
					if (!((start + (k * rpow) + l) < lengths.length)) {
						break;
					}
					sum += lengths[start + (k * rpow) + l];
					i++;
				}
				writer.write(i + "\t" + Math.log10(sum) + "\n");
				writer.flush();
			}
		}
		writer.close();
	}

	public int[] getDocFreqCounts(int max, Map<Integer, List<String>> counts) {
		int[] lengths = new int[max];
		Arrays.fill(lengths, 0);
		for (Map.Entry<Integer, List<String>> entry : counts.entrySet()) {
			lengths[entry.getKey() - 1] = entry.getValue().size();
		}
		return lengths;
	}
	
	public int findDocFreq(String term) throws IOException {
		TermEnum terms = reader.terms();
		boolean next = terms.next();
		while(next) {
			Term t = terms.term();
			if (t.text().equals(term)) {
				return reader.docFreq(t);
			}
			next = terms.next();
		}
		return 0;
	}

	public void printAFew(Map<Integer, List<String>> counts, String regex,
			int number, int count) {
		Iterator<String> onesIterator = counts.get(count).iterator();
		int i = 0;
		Pattern pattern = Pattern.compile(regex);
		Matcher m = null;
		while (i < number && onesIterator.hasNext()) {
			String next = onesIterator.next();
			if (m == null) {
				m = pattern.matcher(next);
			} else {
				m.reset(next);
			}
			if (!m.find()) {
				System.out.println(next);
				i++;
			}
		}
	}

	public static void main(String[] args) throws CorruptIndexException,
			IOException {
		IndexUtils op = new IndexUtils(
				"/home/sumukh/Develop/irtest/contrib/test1/index/gigaword");
/*		Map<String, Object> r = op.getTotalTermsAndTermsByDocFreq();
		int numberOfTerms = (Integer) r.get(TOTAL_NUM_TERMS);
		Map<Integer, List<String>> counts = (Map<Integer, List<String>>) r
				.get(TERMS_BY_DOC_FREQ);
		int maxTermFreq = (Integer) r.get(MAX_TERM_FREQ);

		System.out.println("Number of terms " + numberOfTerms);
		System.out.println("Highest doc freq " + maxTermFreq);
*/
		/*
		 * int[] lengths = op.getDocFreqCounts(maxTermFreq, counts);
		 * 
		 * System.out.println("Number of terms with docFreq " + maxTermFreq + " " +
		 * lengths[lengths.length - 1]); System.out.println("Number of terms
		 * which appear only once " + lengths[0]);
		 */
		// System.out.println("Printing 100 all alphabet character terms that
		// occur only once");
		// op.printAFew(counts, "[0-9]|-|\\'|\\.", 100, 1);
//		op.printFixedLengthTerms(counts, 1	, "[0-9]|[a-z]|\\\\|/", Integer.MAX_VALUE);

		// op.writeCountsToFile(lengths);
		
/*		String regex = "^X$|^X-|^S$|^S-|^CC$|^CC-|^CD$|^CD-|^DT$|^DT-|^EX$|^EX-|^FW$|^FW-|" +
				"^IN$|^IN-|^JJ$|^JJ-|^LS$|^LS-|^MD$|^MD-|^NN$|^NN-|^NP$|^NP-|^NX$|^NX-|^PP$|" +
				"^PP-|^QP$|^QP-|^RB$|^RB-|^RP$|^RP-|^TO$|^TO-|^SQ$|^SQ-|^UH$|^UH-|^VB$|^VB-|" +
				"^VP$|^VP-|^WP$|^WP-|^WRB$|^WRB-|^SYM$|^SYM-|^UCP$|^UCP-|^VBD$|^VBD-|^VBG$|" +
				"^VBG-|^VBN$|^VBN-|^VBP$|^VBP-|^VBZ$|^VBZ-|^WDT$|^WDT-|^RRC$|^RRC-|^RBR$|" +
				"^RBR-|^RBS$|^RBS-|^PDT$|^PDT-|^POS$|^POS-|^NNS$|^NNS-|^NNP$|^NNP-|^LST$|" +
				"^LST-|^JJR$|^JJR-|^JJS$|^JJS-|^NAC$|^NAC-|^PRN$|^PRN-|^PRP$|^PRP-|^PRT$|" +
				"^PRT-|^INTJ$|^INTJ-|^FRAG$|^FRAG-|^ADJP$|^ADJP-|^ADVP$|^ADVP-|^NNPS$|^NNPS-|" +
				"^SBAR$|^SBAR-|^SINV$|^SINV-|^WHNP$|^WHNP-|^WHPP$|^WHPP-|^SBARQ$|^SBARQ-|" +
				"^CONJP$|^CONJP-|^WHADJP$|^WHADJP-|^WHADVP$|^WHADVP-|^PRP$$|^PRP$-|^PRP-S$|" +
				"^PRP-S-|^WP$$|^WP$-|^WP-S$|^WP-S-";
		op.printSelected(counts, regex);
*/
		int numberofDocs = op.findDocFreq("PP-LOC-MNR");
		System.out.println(numberofDocs);
	}

	public void printSelected(Map<Integer, List<String>> counts, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher("");
		for (Integer count : counts.keySet()) {
			List<String> list = counts.get(count);
			for (String term : list) {
				m.reset(term);
				if (m.find()) {
					System.out.println(term + "\t" + count);
				}
			}
		}
		
	}

	public void printFixedLengthTerms(Map<Integer, List<String>> counts,
			int length, String regex, int num) throws IOException {
		Writer f = new FileWriter("length" + length);
		int n = 0;
		Pattern pattern = Pattern.compile(regex);
		Matcher m = null;
		for (Integer count : counts.keySet()) {
			List<String> termlist = counts.get(count);
			if (n >= num)
				break;
			for (String term : termlist) {
				if (n >= num)
					break;
				if (term.length() == length) {
					if (m == null) {
						m = pattern.matcher(term);
					} else {
						m.reset(term);
					}
					if (!m.find()) {
						f.write(n++ + "\t" + term + "\t" + count + "\n");
					}
				}
			}
		}
		f.close();

	}
}
