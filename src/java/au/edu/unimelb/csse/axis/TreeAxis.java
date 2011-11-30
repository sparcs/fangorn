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
package au.edu.unimelb.csse.axis;

import java.util.HashMap;

import au.edu.unimelb.csse.search.join.EarlyStopJoin;
import au.edu.unimelb.csse.search.join.EarlyStopWithFCJoin;
import au.edu.unimelb.csse.search.join.FilterJoin;
import au.edu.unimelb.csse.search.join.FilterJoinAware;
import au.edu.unimelb.csse.search.join.LookaheadOptimization;
import au.edu.unimelb.csse.search.join.SimpleJoin;
import au.edu.unimelb.csse.search.join.SimpleWithFCJoin;
import au.edu.unimelb.csse.search.join.TermJoinAware;
import au.edu.unimelb.csse.search.join.TermJoinType;

public interface TreeAxis {

	String name();

	TermJoinAware termJoiner(TermJoinType joinType);

	FilterJoinAware filterJoiner();

	int id();

	TreeAxis conjugate();

	TreeAxis DESCENDANT = new TreeAxisImpl(0, "//",
			"DESCENDANT", SimpleJoin.DESCENDANT,
			SimpleWithFCJoin.DESCENDANT, EarlyStopJoin.DESCENDANT,
			EarlyStopWithFCJoin.DESCENDANT, FilterJoin.DESCENDANT, LookaheadOptimization.RETAIN_ANCESTOR);

	TreeAxis ANCESTOR = new TreeAxisImpl(1, "\\\\", "ANCESTOR",
			SimpleJoin.ANCESTOR, SimpleWithFCJoin.ANCESTOR,
			EarlyStopJoin.ANCESTOR, EarlyStopWithFCJoin.ANCESTOR,
			FilterJoin.ANCESTOR, LookaheadOptimization.RETAIN_CHILDREN);

	TreeAxis CHILD = new TreeAxisImpl(2, "/", "CHILD",
			SimpleJoin.CHILD, SimpleWithFCJoin.CHILD,
			EarlyStopJoin.CHILD, EarlyStopWithFCJoin.CHILD, FilterJoin.CHILD);

	TreeAxis PARENT = new TreeAxisImpl(3, "\\", "PARENT",
			SimpleJoin.PARENT, SimpleWithFCJoin.PARENT,
			EarlyStopJoin.PARENT, EarlyStopWithFCJoin.PARENT, FilterJoin.PARENT);

	TreeAxis FOLLOWING_SIBLING = new TreeAxisImpl(4, "==>",
			"FOLLOWING_SIBLING", SimpleJoin.FOLLOWING_SIBLING,
			SimpleWithFCJoin.FOLLOWING_SIBLING,
			EarlyStopJoin.FOLLOWING_SIBLING, EarlyStopWithFCJoin.FOLLOWING_SIBLING, FilterJoin.FOLLOWING_SIBLING);

	TreeAxis PRECEDING_SIBLING = new TreeAxisImpl(5, "<==",
			"PRECEDING_SIBLING", SimpleJoin.PRECEDING_SIBLING,
			SimpleWithFCJoin.PRECEDING_SIBLING,
			EarlyStopJoin.PRECEDING_SIBLING, EarlyStopWithFCJoin.PRECEDING_SIBLING, FilterJoin.PRECEDING_SIBLING);

	TreeAxis IMMEDIATE_FOLLOWING_SIBLING = new TreeAxisImpl(6,
			"=>",
			"IMMEDIATE_FOLLOWING_SIBLING",
			SimpleJoin.IMMEDIATE_FOLLOWING_SIBLING,
			SimpleWithFCJoin.IMMEDIATE_FOLLOWING_SIBLING,
			EarlyStopJoin.IMMEDIATE_FOLLOWING_SIBLING,
			EarlyStopWithFCJoin.IMMEDIATE_FOLLOWING_SIBLING, FilterJoin.IMMEDIATE_FOLLOWING_SIBLING);

	TreeAxis IMMEDIATE_PRECEDING_SIBLING = new TreeAxisImpl(7,
			"<=",
			"IMMEDIATE_PRECEDING_SIBLING",
			SimpleJoin.IMMEDIATE_PRECEDING_SIBLING,
			SimpleWithFCJoin.IMMEDIATE_PRECEDING_SIBLING,
			EarlyStopJoin.IMMEDIATE_PRECEDING_SIBLING,
			EarlyStopWithFCJoin.IMMEDIATE_PRECEDING_SIBLING, FilterJoin.IMMEDIATE_PRECEDING_SIBLING);

	TreeAxis FOLLOWING = new TreeAxisImpl(8, "-->", "FOLLOWING",
			SimpleJoin.FOLLOWING, SimpleWithFCJoin.FOLLOWING,
			EarlyStopJoin.FOLLOWING, EarlyStopWithFCJoin.FOLLOWING,
			FilterJoin.FOLLOWING, LookaheadOptimization.RETAIN_LEFTMOST);

	TreeAxis PRECEDING = new TreeAxisImpl(9, "<--", "PRECEDING",
			SimpleJoin.PRECEDING, SimpleWithFCJoin.PRECEDING,
			EarlyStopJoin.PRECEDING, EarlyStopWithFCJoin.PRECEDING,
			FilterJoin.PRECEDING, LookaheadOptimization.RETAIN_RIGHTMOST);

	TreeAxis IMMEDIATE_FOLLOWING = new TreeAxisImpl(10, "->",
			"IMMEDIATE_FOLLOWING",
			SimpleJoin.IMMEDIATE_FOLLOWING,
			SimpleWithFCJoin.IMMEDIATE_FOLLOWING,
			EarlyStopJoin.IMMEDIATE_FOLLOWING,
			EarlyStopWithFCJoin.IMMEDIATE_FOLLOWING, FilterJoin.IMMEDIATE_FOLLOWING);

	TreeAxis IMMEDIATE_PRECEDING = new TreeAxisImpl(11, "<-",
			"IMMEDIATE_PRECEDING",
			SimpleJoin.IMMEDIATE_PRECEDING,
			SimpleWithFCJoin.IMMEDIATE_PRECEDING,
			EarlyStopJoin.IMMEDIATE_PRECEDING,
			EarlyStopWithFCJoin.IMMEDIATE_PRECEDING, FilterJoin.IMMEDIATE_PRECEDING);

	final class ConjugateResolver extends HashMap<TreeAxis, TreeAxis> {

		private static final long serialVersionUID = -117956706816148789L;

		private ConjugateResolver() {
			this.put(TreeAxis.CHILD, TreeAxis.PARENT);
			this.put(TreeAxis.PARENT, TreeAxis.CHILD);
			this.put(TreeAxis.DESCENDANT, TreeAxis.ANCESTOR);
			this.put(TreeAxis.ANCESTOR, TreeAxis.DESCENDANT);
			this.put(TreeAxis.FOLLOWING, TreeAxis.PRECEDING);
			this.put(TreeAxis.PRECEDING, TreeAxis.FOLLOWING);
			this.put(TreeAxis.FOLLOWING_SIBLING, TreeAxis.PRECEDING_SIBLING);
			this.put(TreeAxis.PRECEDING_SIBLING, TreeAxis.FOLLOWING_SIBLING);
			this
					.put(TreeAxis.IMMEDIATE_FOLLOWING,
							TreeAxis.IMMEDIATE_PRECEDING);
			this
					.put(TreeAxis.IMMEDIATE_PRECEDING,
							TreeAxis.IMMEDIATE_FOLLOWING);
			this.put(TreeAxis.IMMEDIATE_FOLLOWING_SIBLING,
					TreeAxis.IMMEDIATE_PRECEDING_SIBLING);
			this.put(TreeAxis.IMMEDIATE_PRECEDING_SIBLING,
					TreeAxis.IMMEDIATE_FOLLOWING_SIBLING);
		}
	}

	ConjugateResolver CONJUGATE = new ConjugateResolver();

	LookaheadOptimization lookaheadOptimization();

	String symbol();
}
