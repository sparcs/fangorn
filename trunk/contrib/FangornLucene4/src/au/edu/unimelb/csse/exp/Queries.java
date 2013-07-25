package au.edu.unimelb.csse.exp;

import au.edu.unimelb.csse.Operator;

class Queries {
	public static final int SIZE = 45;

	public TreeQuery getQuery(int pos) {
		TreeQuery q = new TreeQuery();
		switch (pos) {
		case 0:
			q.setLabels("NP", "VP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 1:
			q.setLabels("NP", "VP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 2:
			q.setLabels("VP", "NP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 3:
			q.setLabels("VP", "NP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 4:
			q.setLabels("S", "PP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 5:
			q.setLabels("S", "PP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 6:
			q.setLabels("PP", "S");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 7:
			q.setLabels("PP", "S");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 8:
			q.setLabels("S", "DT");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 9:
			q.setLabels("S", "DT");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 10:
			q.setLabels("DT", "S");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 11:
			q.setLabels("DT", "S");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 12:
			q.setLabels("UCP", "ADVP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 13:
			q.setLabels("UCP", "ADVP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 14:
			q.setLabels("ADVP", "UCP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 15:
			q.setLabels("ADVP", "UCP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 16:
			q.setLabels("CONJP", "RB");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD);
			break;
		case 17:
			q.setLabels("PP", "these");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 18:
			q.setLabels("WHPP", "at");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 19:
			q.setLabels("PRN", ",", "ADJP", "PP");
			q.setParents(-1, 0, 0, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 20:
			q.setLabels("PRN", "ADJP", ",", "PP");
			q.setParents(-1, 0, 0, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 21:
			q.setLabels("PRN", "PP", "ADJP", ",");
			q.setParents(-1, 0, 0, 0);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 22:
			q.setLabels("VP", "JJ", "ADJP", "PP", "NP");
			q.setParents(-1, 0, 0, 0, 3);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD, Operator.CHILD,
					Operator.CHILD, Operator.CHILD);
			break;
		case 23:
			q.setLabels("VP", "ADJP", "JJ", "PP", "NP");
			q.setParents(-1, 0, 0, 0, 3);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD, Operator.CHILD,
					Operator.CHILD, Operator.CHILD);
			break;
		case 24:
			q.setLabels("VP", "PP", "NP", "ADJP", "JJ");
			q.setParents(-1, 0, 1, 0, 0);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD, Operator.CHILD,
					Operator.CHILD, Operator.CHILD);
			break;
		case 25:
			q.setLabels("S", "VP", "PP", "NP", "VBN", "IN");
			q.setParents(-1, 0, 1, 2, 3, 2);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD,
					Operator.DESCENDANT, Operator.DESCENDANT, Operator.CHILD,
					Operator.CHILD);
			break;
		case 26:
			q.setLabels("S", "VP", "PP", "NP", "VBN", "IN");
			q.setParents(-1, 0, 1, 2, 3, 2);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD, Operator.CHILD,
					Operator.CHILD, Operator.CHILD, Operator.CHILD);
			break;
		case 27:
			q.setLabels("S", "VP", "PP", "IN", "NP", "VBN");
			q.setParents(-1, 0, 1, 2, 2, 4);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD, Operator.CHILD,
					Operator.CHILD, Operator.CHILD, Operator.CHILD);
			break;
		case 28:
			q.setLabels("S", "VP", "PP", "NN", "NP", "CD", "VBN", "IN");
			q.setParents(-1, 0, 1, 2, 2, 4, 4, 2);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD,
					Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT, Operator.DESCENDANT, Operator.CHILD,
					Operator.CHILD);
			break;
		case 29:
			q.setLabels("VP", "NP", "PP");
			q.setParents(-1, 0, 1);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT);
			break;
		case 30:
			q.setLabels("VP", "NP", "PP", "of");
			q.setParents(-1, 0, 1, 2);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 31:
			q.setLabels("VP", "NP", "PP", "of");
			q.setParents(-1, 0, 1, 2);
			q.setOperators(Operator.DESCENDANT, Operator.CHILD,
					Operator.DESCENDANT, Operator.DESCENDANT);
			break;
		case 32:
			q.setLabels("VP", "PP", "UCP");
			q.setParents(-1, 0, 1);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT);
			break;
		case 33:
			q.setLabels("NP", "PP", "UCP");
			q.setParents(-1, 0, 1);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT);
			break;
		case 34:
			q.setLabels("PP", "UCP", "VP");
			q.setParents(-1, 0, 1);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.DESCENDANT);
			break;
		case 35:
			q.setLabels("PP", "UCP", "VP");
			q.setParents(-1, 0, 1);
			q.setOperators(Operator.DESCENDANT, Operator.DESCENDANT,
					Operator.CHILD);
			break;
		case 36:
			q.setLabels("go", "on", "NP");
			q.setParents(-1, 0, 1);
			q.setOperators(Operator.DESCENDANT, Operator.IMMEDIATE_FOLLOWING,
					Operator.IMMEDIATE_FOLLOWING);
			break;
		case 37:
			q.setLabels("VB", "ADVP", "RB", "right", "RB");
			q.setParents(-1, 0, 1, 2, 2);
			q.setOperators(Operator.DESCENDANT, Operator.FOLLOWING_SIBLING,
					Operator.CHILD, Operator.CHILD,
					Operator.IMMEDIATE_FOLLOWING_SIBLING);
			break;
		case 38:
			q.setLabels("VP", "NP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.FOLLOWING_SIBLING);
			break;
		case 39:
			q.setLabels("NP", "VP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.PRECEDING_SIBLING);
			break;
		case 40:
			q.setLabels("VP", "NP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.PRECEDING_SIBLING);
			break;
		case 41:
			q.setLabels("NP", "VP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT, Operator.FOLLOWING_SIBLING);
			break;
		case 42:
			q.setLabels("VP", "NP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT,
					Operator.IMMEDIATE_FOLLOWING_SIBLING);
			break;
		case 43:
			q.setLabels("VP", "NP");
			q.setParents(-1, 0);
			q.setOperators(Operator.DESCENDANT,
					Operator.IMMEDIATE_PRECEDING_SIBLING);
			break;
		case 44:
			q.setLabels("JJ", "NN", "CC", "and", "NN", "NNS");
			q.setParents(-1, 0, 1, 2, 2, 4);
			q.setOperators(Operator.DESCENDANT,
					Operator.IMMEDIATE_FOLLOWING_SIBLING,
					Operator.IMMEDIATE_FOLLOWING_SIBLING, Operator.CHILD,
					Operator.IMMEDIATE_FOLLOWING_SIBLING,
					Operator.FOLLOWING_SIBLING);
			break;
		default:
			break;
		}
		return q;

	}
}
