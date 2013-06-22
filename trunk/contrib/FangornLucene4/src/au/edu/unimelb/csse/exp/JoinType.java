package au.edu.unimelb.csse.exp;

import au.edu.unimelb.csse.join.Baseline1Join;
import au.edu.unimelb.csse.join.Baseline2Join;
import au.edu.unimelb.csse.join.BooleanJoinPipeline;
import au.edu.unimelb.csse.join.ComputesBooleanResult;
import au.edu.unimelb.csse.join.ComputesFullResults;
import au.edu.unimelb.csse.join.HalfPairJoinPipeline;
import au.edu.unimelb.csse.join.LookaheadTermEarlyJoin;
import au.edu.unimelb.csse.join.LookaheadTermEarlyMRRJoin;
import au.edu.unimelb.csse.join.LookaheadTermEarlyPipeline;
import au.edu.unimelb.csse.join.MPMGJoin;
import au.edu.unimelb.csse.join.MPMGMRRJoin;
import au.edu.unimelb.csse.join.MPMGModJoin;
import au.edu.unimelb.csse.join.MPMGModSingleJoin;
import au.edu.unimelb.csse.join.PathStackJoin;
import au.edu.unimelb.csse.join.StackTreeJoin;
import au.edu.unimelb.csse.join.StaircaseJoin;
import au.edu.unimelb.csse.join.StructuredBooleanPathJoin;
import au.edu.unimelb.csse.join.StructuredFullPathJoin;
import au.edu.unimelb.csse.join.TwigStackJoin;
import au.edu.unimelb.csse.paypack.LogicalNodePositionAware;

public enum JoinType {
	BASELINE1(0, true, true) {
		@Override
		public ComputesFullResults getFullJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			return new StructuredFullPathJoin(query.labels(), query.parents(),
					query.operators(), new Baseline1Join(nodePositionAware),
					nodePositionAware);
		}
	},
	BASELINE2(1, false, true) {
		@Override
		public ComputesBooleanResult getBooleanJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(
					nodePositionAware, Baseline2Join.JOIN_BUILDER);
			return new StructuredBooleanPathJoin(query.labels(),
					query.parents(), query.operators(), pipeline,
					nodePositionAware);
		}
	},
	MPMG1(2, true, true) {
		@Override
		public ComputesFullResults getFullJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			return new StructuredFullPathJoin(query.labels(), query.parents(),
					query.operators(), new MPMGJoin(nodePositionAware),
					nodePositionAware);
		}
	},
	MPMG2(3, true, true) {
		@Override
		public ComputesFullResults getFullJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			return new StructuredFullPathJoin(query.labels(), query.parents(),
					query.operators(), new MPMGModJoin(nodePositionAware),
					nodePositionAware);
		}
	},
	STACKTREE(4, true, true) {
		@Override
		public ComputesFullResults getFullJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			return new StructuredFullPathJoin(query.labels(), query.parents(),
					query.operators(), new StackTreeJoin(nodePositionAware),
					nodePositionAware);
		}
	},
	MPMG3(5, false, true) {
		@Override
		public ComputesBooleanResult getBooleanJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(
					nodePositionAware, MPMGModSingleJoin.JOIN_BUILDER);
			return new StructuredBooleanPathJoin(query.labels(),
					query.parents(), query.operators(), pipeline,
					nodePositionAware);
		}
	},
	MPMG4(6, false, true) {
		@Override
		public ComputesBooleanResult getBooleanJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(
					nodePositionAware, MPMGMRRJoin.JOIN_BUILDER);
			return new StructuredBooleanPathJoin(query.labels(),
					query.parents(), query.operators(), pipeline,
					nodePositionAware);
		}
	},
	STAIRCASE(7, false, true) {
		@Override
		public ComputesBooleanResult getBooleanJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			HalfPairJoinPipeline pipeline = new HalfPairJoinPipeline(
					nodePositionAware, StaircaseJoin.JOIN_BUILDER);
			return new StructuredBooleanPathJoin(query.labels(),
					query.parents(), query.operators(), pipeline,
					nodePositionAware);
		}
	},
	LATE(8, false, true) {
		@Override
		public ComputesBooleanResult getBooleanJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			BooleanJoinPipeline pipeline = new LookaheadTermEarlyPipeline(
					nodePositionAware, LookaheadTermEarlyJoin.JOIN_BUILDER);
			return new StructuredBooleanPathJoin(query.labels(),
					query.parents(), query.operators(), pipeline,
					nodePositionAware);
		}
	},
	LATEMRR(9, false, true) {
		@Override
		public ComputesBooleanResult getBooleanJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			BooleanJoinPipeline pipeline = new LookaheadTermEarlyPipeline(
					nodePositionAware, LookaheadTermEarlyMRRJoin.JOIN_BUILDER);
			return new StructuredBooleanPathJoin(query.labels(),
					query.parents(), query.operators(), pipeline,
					nodePositionAware);
		}
	},
	TWIGSTACK(10, true, true) {
		@Override
		public ComputesFullResults getFullJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			return new TwigStackJoin(query.labels(), query.parents(),
					query.operators(), nodePositionAware);
		}
	},
	PATHSTACK(11, true, false) {
		@Override
		public ComputesFullResults getFullJoin(TreeQuery query,
				LogicalNodePositionAware nodePositionAware) {
			return new PathStackJoin(query.labels(), query.operators(),
					nodePositionAware);
		}
	};

	private boolean fullResults;
	private boolean allowsBranches;
	private int id;

	private JoinType(int num, boolean fullResults, boolean allowsBranches) {
		this.id = num;
		this.fullResults = fullResults;
		this.allowsBranches = allowsBranches;
	}

	public boolean allowsBranches() {
		return allowsBranches;
	}

	public boolean returnsFullResults() {
		return fullResults;
	}

	public int getId() {
		return id;
	}

	public ComputesFullResults getFullJoin(TreeQuery query,
			LogicalNodePositionAware nodePositionAware) {
		return null;
	}

	public ComputesBooleanResult getBooleanJoin(TreeQuery query,
			LogicalNodePositionAware nodePositionAware) {
		return null;
	}
}
