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

import java.io.Serializable;

import au.edu.unimelb.csse.search.join.FilterJoinAware;
import au.edu.unimelb.csse.search.join.LookaheadOptimization;
import au.edu.unimelb.csse.search.join.TermJoinAware;
import au.edu.unimelb.csse.search.join.TermJoinType;

class TreeAxisImpl implements TreeAxis, Serializable {
	private static final long serialVersionUID = 151911127907301764L;
	private final int id;
	private final String name;
	private final TermJoinAware simpleJoin;
	private final FilterJoinAware filterJoin;
	private final TermJoinAware simpleJoinWithFC;
	private final TermJoinAware earlyStopJoin;
	private final TermJoinAware earlyStopJoinWithFC;
	private final LookaheadOptimization lookaheadOptimization;
	private final String symbol;
	
	TreeAxisImpl(int id, String symbol, String name, TermJoinAware simpleJoin, TermJoinAware simpleJoinWithFC, TermJoinAware earlyStopJoin, TermJoinAware earlyStopJoinWithFC, FilterJoinAware filterJoin) {
		this(id, symbol, name, earlyStopJoinWithFC, earlyStopJoinWithFC, earlyStopJoinWithFC, earlyStopJoinWithFC, filterJoin, LookaheadOptimization.NONE);
	}
	
	TreeAxisImpl(int id, String symbol, String name, TermJoinAware simpleJoin, TermJoinAware simpleJoinWithFC, TermJoinAware earlyStopJoin, TermJoinAware earlyStopJoinWithFC, FilterJoinAware filterJoin, LookaheadOptimization optimization) {
		this.id = id;
		this.name = name;
		this.simpleJoin = simpleJoin;
		this.simpleJoinWithFC = simpleJoinWithFC;
		this.earlyStopJoin = earlyStopJoin;
		this.earlyStopJoinWithFC = earlyStopJoinWithFC;
		this.filterJoin = filterJoin;
		this.lookaheadOptimization = optimization;
		this.symbol = symbol;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public TermJoinAware termJoiner(TermJoinType joinType) {
		if (TermJoinType.EARLY_STOP_WITH_FC.equals(joinType)) {
			return earlyStopJoinWithFC;
		} else if (TermJoinType.EARLY_STOP.equals(joinType)) {
			return earlyStopJoin;
		} else if (TermJoinType.SIMPLE_WITH_FC.equals(joinType)) {
			return simpleJoinWithFC;
		} else if (TermJoinType.SIMPLE.equals(joinType)) {
			return simpleJoin;
		}
		return earlyStopJoinWithFC;
	}

	@Override
	public FilterJoinAware filterJoiner() {
		return filterJoin;
	}
	
	@Override
	public int id() {
		return this.id;
	}

	@Override
	public String toString() {
		return name();
	}
	
	public TreeAxis conjugate() {
		return TreeAxis.CONJUGATE.get(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((simpleJoin == null) ? 0 : simpleJoin.hashCode());
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TreeAxisImpl))
			return false;
		TreeAxisImpl other = (TreeAxisImpl) obj;
//		if (simpleJoin == null) {
//			if (other.simpleJoin != null)
//				return false;
//		} else if (!simpleJoin.equals(other.simpleJoin))
//			return false;
		//Just compare name and id
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public LookaheadOptimization lookaheadOptimization() {
		return lookaheadOptimization;
	}

	@Override
	public String symbol() {
		return symbol;
	}

	
}
