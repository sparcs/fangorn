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
package au.edu.unimelb.csse.search.join;

import java.util.BitSet;

import au.edu.unimelb.csse.search.NodeDataBuffer;

/**
 * This interface contains all the filter joins.
 * 
 * Filter joins differ from term joins as they join 2 sequences of 
 * @author Sumukh Ghodke
 *
 */
public interface FilterJoin {
	FilterJoinAware DESCENDANT = new FilterJoinAwareImpl(JoinLogic.DESCENDANT, false);

	FilterJoinAware ANCESTOR = new FilterJoinAwareImpl(JoinLogic.ANCESTOR, true);

	FilterJoinAware CHILD = new FilterJoinAwareImpl(JoinLogic.CHILD, false);

	FilterJoinAware PARENT = new FilterJoinAwareImpl(JoinLogic.PARENT, true);

	FilterJoinAware FOLLOWING_SIBLING = new FilterJoinAwareImpl(JoinLogic.FOLLOWING_SIBLING, false);

	FilterJoinAware PRECEDING_SIBLING = new FilterJoinAwareImpl(JoinLogic.PRECEDING_SIBLING, true);

	FilterJoinAware IMMEDIATE_FOLLOWING_SIBLING = new FilterJoinAwareImpl(JoinLogic.IMMEDIATE_FOLLOWING_SIBLING, false);

	FilterJoinAware IMMEDIATE_PRECEDING_SIBLING = new FilterJoinAwareImpl(JoinLogic.IMMEDIATE_PRECEDING_SIBLING, true);

	FilterJoinAware FOLLOWING = new FilterJoinAwareImpl(JoinLogic.FOLLOWING, false);

	FilterJoinAware PRECEDING = new FilterJoinAwareImpl(JoinLogic.PRECEDING, true);

	FilterJoinAware IMMEDIATE_FOLLOWING = new FilterJoinAwareImpl(JoinLogic.IMMEDIATE_FOLLOWING, false);

	FilterJoinAware IMMEDIATE_PRECEDING = new FilterJoinAwareImpl(JoinLogic.IMMEDIATE_PRECEDING, true);
}

class FilterJoinAwareImpl implements FilterJoinAware {
	private JoinLogicAware joinAware;
	private boolean firstToLast;
	
	FilterJoinAwareImpl(JoinLogicAware joinAware, boolean firstToLast) {
		this.joinAware = joinAware;
		this.firstToLast = firstToLast;
	}

	@Override
	public boolean matchFilterHead(NodeDataBuffer parent,
			NodeDataBuffer filterHead, BitSet matchedInParent, boolean isNot) {
		return parent.match(filterHead, matchedInParent, isNot, joinAware, firstToLast);
	}
}
