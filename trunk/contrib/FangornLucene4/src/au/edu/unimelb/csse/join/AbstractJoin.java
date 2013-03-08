package au.edu.unimelb.csse.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.ArrayUtil;

import au.edu.unimelb.csse.BinaryOperator;
import au.edu.unimelb.csse.Constants;

/**
 * 
 * @author sumukh
 * 
 */
abstract class AbstractJoin {

	protected String[] labels;
	protected PostingsAndFreq[] postingsFreqs;
	protected int[] parentPos; // indexed by postingsFreqs.position
	protected int currentContextPos = -1;
	protected int atomicContextsCount;
	Iterator<AtomicReaderContext> contextIter;

	protected int docID = -1;
	protected BinaryOperator[] operators;
	protected PostingsAndFreq root;

	static class PostingsAndFreq implements Comparable<PostingsAndFreq> {
		DocsAndPositionsEnum postings;
		int docFreq;
		int position;
		Term term;
		boolean useAdvance;
		boolean isLeaf = false;
		PostingsAndFreq[] children;
		PostingsAndFreq parent;

		public PostingsAndFreq(DocsAndPositionsEnum postings, int docFreq,
				int position, Term term) {
			this.postings = postings;
			this.docFreq = docFreq;
			this.position = position; // position of the term in the query
			this.term = term;
		}

		@Override
		public int compareTo(PostingsAndFreq o) {
			if (docFreq != o.docFreq) {
				return docFreq - o.docFreq;
			}
			if (position != o.position) {
				return position - o.position;
			}
			// positions can be same only on synonyms/regexed terms
			if (term != o.term) {
				return term.compareTo(o.term);
			}
			if (useAdvance != o.useAdvance) {
				return new Boolean(useAdvance).compareTo(o.useAdvance);
			}
			return 0;
		}

		// this hashcode method does not compare the parent and child
		// PostingsAndFreq objects within
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + docFreq;
			result = prime * result + position;
			result = prime * result + (useAdvance ? 0 : 1);
			result = prime * result
					+ ((postings == null) ? 0 : postings.hashCode());
			result = prime * result + ((term == null) ? 0 : term.hashCode());
			return result;
		}

		// this equals method does not compare the parent and child
		// PostingsAndFreq objects within
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PostingsAndFreq other = (PostingsAndFreq) obj;
			if (docFreq != other.docFreq || position != other.position
					|| useAdvance != other.useAdvance)
				return false;
			if (postings == null) {
				if (other.postings != null)
					return false;
			} else if (!postings.equals(other.postings))
				return false;
			if (term == null) {
				if (other.term != null)
					return false;
			} else if (!term.equals(other.term))
				return false;
			return true;
		}

		public void setUseAdvance(boolean useAdvance) {
			this.useAdvance = useAdvance;
		}

		public void reset(DocsAndPositionsEnum postings, int docFreq,
				int position, Term term) {
			this.postings = postings;
			this.docFreq = docFreq;
			this.position = position;
			this.term = term;
			isLeaf = false;
			useAdvance = false;
		}
	}

	AbstractJoin(String[] labels, BinaryOperator[] operators) {
		this(labels, getPathPositions(labels), operators);
	}

	private static int[] getPathPositions(String[] labels) {
		int[] parentPos = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			parentPos[i] = i - 1;
		}
		return parentPos;
	}

	AbstractJoin(String[] labels, int[] parentPos, BinaryOperator[] operators) {
		this.labels = labels;
		this.parentPos = parentPos;
		this.postingsFreqs = new PostingsAndFreq[labels.length];
		this.operators = operators;
	}

	public boolean setup(IndexReader r) throws IOException {
		currentContextPos = -1;
		List<AtomicReaderContext> leaves = r.getContext().leaves();
		atomicContextsCount = leaves.size();
		contextIter = leaves.iterator();
		return initAtomicContextPositionsFreqs();
	}

	boolean isAtLastContext() {
		// can never be greater
		return currentContextPos == atomicContextsCount - 1;
	}

	int nextDocAtomicContext() throws IOException {
		while (true) {
			// first (rarest) term
			final int doc = postingsFreqs[0].postings.nextDoc();
			if (doc == DocIdSetIterator.NO_MORE_DOCS) {
				docID = doc;
				return doc;
			}

			// not-first terms
			int i = 1;
			while (i < postingsFreqs.length) {
				final PostingsAndFreq cs = postingsFreqs[i];
				int doc2 = cs.postings.docID();
				if (cs.useAdvance) {
					if (doc2 < doc) {
						doc2 = cs.postings.advance(doc);
					}
				} else {
					int iter = 0;
					while (doc2 < doc) {
						// safety net -- fallback to .advance if we've
						// done too many .nextDocs
						if (++iter == 50) {
							doc2 = cs.postings.advance(doc);
							break;
						} else {
							doc2 = cs.postings.nextDoc();
						}
					}
				}
				if (doc2 > doc) {
					break;
				}
				i++;
			}

			if (i == postingsFreqs.length) {
				// this doc has all the terms
				docID = doc;
				return doc;
			}
		}
	}

	// will return nextDoc id without need to interact with AtomicContext
	public int nextDoc() throws IOException {
		int doc = DocIdSetIterator.NO_MORE_DOCS;
		while ((doc = nextDocAtomicContext()) == DocIdSetIterator.NO_MORE_DOCS) {
			if (!isAtLastContext()) {
				if (!initAtomicContextPositionsFreqs()) {
					return DocIdSetIterator.NO_MORE_DOCS;
				}
				continue;
			}
			break;
		}
		if (doc != DocIdSetIterator.NO_MORE_DOCS) {
			setupPerDoc();
		}
		return doc;
	}

	/*
	 * will return false when any query term is not found in the current context
	 * OR when there are no more atomic contexts to read
	 */
	boolean initAtomicContextPositionsFreqs() throws IOException {
		boolean foundAll = false;
		// posEnum is in the same order as the labels in the query
		final DocsAndPositionsEnum[] posEnum = new DocsAndPositionsEnum[labels.length];
		final Term[] terms = new Term[labels.length];
		final int[] docFreq = new int[labels.length];
		while (!foundAll && contextIter.hasNext()) {
			foundAll = true;
			AtomicReader reader = contextIter.next().reader();
			final Terms fieldTerms = reader.terms(Constants.FIELD_NAME);
			final TermsEnum termsIterator = fieldTerms.iterator(null);
			for (int i = 0; i < labels.length; i++) {
				String label = labels[i];
				terms[i] = new Term(Constants.FIELD_NAME, label);
				boolean seek = termsIterator.seekExact(terms[i].bytes(), true);
				if (!seek) {
					foundAll = false;
					break;
				}
				docFreq[i] = termsIterator.docFreq();
				posEnum[i] = reader.termPositionsEnum(terms[i]);
				if (posEnum[i] == null) {
					foundAll = false;
					break;
				}
			}
			currentContextPos++;
		}
		if (foundAll) {
			for (int i = 0; i < posEnum.length; i++) {
				if (postingsFreqs[i] == null) {
					postingsFreqs[i] = new PostingsAndFreq(posEnum[i],
							docFreq[i], i, terms[i]);
					if (parentPos[i] == -1) {
						root = postingsFreqs[i];
					}
				} else {
					postingsFreqs[i].reset(posEnum[i], docFreq[i], i, terms[i]);
				}
			}
			setParentChild();
			// so far the postingsFreqs is ordered in the same arrangement as
			// the query terms; after the mergesort the order changes; the
			// PostingsAndFreq object contains the position field that stores
			// the position of the PostingsAndFreq term in the query but an
			// inverse mapping from the query order to index on a particular
			// PostingsAndFreq object in postingsFreqs is required in
			// TwigStackJoin only
			ArrayUtil.mergeSort(postingsFreqs);
			for (int i = 0; i < postingsFreqs.length; i++) {
				// Optimization copied from Lucene phrase query
				// Coarse optimization: advance(target) is fairly
				// costly, so, if the relative freq of the 2nd
				// rarest term is not that much (> 1/5th) rarer than
				// the first term, then we just use .nextDoc() when
				// ANDing. This buys ~15% gain where
				// freq of rarest 2 terms is close:
				final boolean useAdvance = postingsFreqs[i].docFreq > 5 * postingsFreqs[0].docFreq;
				postingsFreqs[i].setUseAdvance(useAdvance);
			}
			setupPerAtomicContext();
		}
		return foundAll;
	}

	private void setParentChild() {
		for (int i = 0; i < postingsFreqs.length; i++) {
			int pos = postingsFreqs[i].position;
			if (parentPos[pos] != -1) {
				postingsFreqs[i].parent = postingsFreqs[parentPos[pos]];
			}
			List<PostingsAndFreq> children = new ArrayList<AbstractJoin.PostingsAndFreq>();
			for (int j = 0; j < postingsFreqs.length; j++) {
				if (j == i) {
					continue;
				}
				if (parentPos[j] == pos) {
					children.add(postingsFreqs[j]);
				}
			}
			postingsFreqs[i].children = children
					.toArray(new PostingsAndFreq[children.size()]);
		}
	}

	/*
	 * this should ideally be PostingsAndFreq object's job but asking it to do
	 * so will involve extra work each time shouldStop() is called in
	 * AbstractHolisticJoin; need to find a workaround
	 */
	private void identifyLeaves() {
		for (PostingsAndFreq pf : postingsFreqs) {
			if (pf.children == null || pf.children.length == 0) {
				pf.isLeaf = true;
			}
		}
	}

	public void setupPerAtomicContext() {
		identifyLeaves();
	}

	public abstract void setupPerDoc() throws IOException;
}
