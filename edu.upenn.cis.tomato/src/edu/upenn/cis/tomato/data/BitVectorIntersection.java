package edu.upenn.cis.tomato.data;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.util.intset.MutableSharedBitVectorIntSet;

/**
 * Operator U(n) = U(n) ^ U(j)
 */
public class BitVectorIntersection extends
		AbstractMeetOperator<BitVectorVariable> implements FixedPointConstants {

	private final static BitVectorIntersection SINGLETON = new BitVectorIntersection();

	private final static boolean Flow_DEBUG = false;

	public static BitVectorIntersection instance() {
		return SINGLETON;
	}

	private BitVectorIntersection() {
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Intersection";
	}

	@Override
	public int hashCode() {
		return 9901;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof BitVectorIntersection);
	}

	/*
	 * @see
	 * com.ibm.wala.dataflow.fixpoint.Operator#evaluate(com.ibm.wala.dataflow
	 * .fixpoint.IVariable[])
	 */
	@Override
	public byte evaluate(BitVectorVariable lhs, BitVectorVariable[] rhs)
			throws IllegalArgumentException {

		if (lhs == null) {
			throw new IllegalArgumentException("null lhs");
		}
		if (rhs == null) {
			throw new IllegalArgumentException("null rhs");
		}

		BitVectorVariable U = new BitVectorVariable();
		U.copyState(lhs);

		int rhsize = rhs.length;
		if (rhsize == 0) {
			U.V = new MutableSharedBitVectorIntSet();
		} else if (rhsize == 1) {
			if (rhs[0].getValue() != null) {
				U.addAll(rhs[0]);
			} else {
				U.V = new MutableSharedBitVectorIntSet();
			}
		} else {
			MutableSharedBitVectorIntSet result = null;

			for (int i = 0; i < rhs.length; i++) {
				if (rhs[i].getValue() != null) {
					result = new MutableSharedBitVectorIntSet(
							(MutableSharedBitVectorIntSet) rhs[i].getValue());
					break;
				}
			}

			if (result == null) {
				U.V = new MutableSharedBitVectorIntSet();
			} else {
				if (Flow_DEBUG) {
					System.out.println("Intersection of ");
				}

				for (int i = 0; i < rhs.length; i++) {
					if (rhs[i].getValue() != null) {
						if (Flow_DEBUG) {
							System.out.println(rhs[i].getValue());
						}
						result.intersectWith(rhs[i].getValue());
					}
				}

				U.V = result;
			}

			if (Flow_DEBUG) {
				System.out.println("is " + U.getValue());
			}
		}

		if (!lhs.sameValue(U)) {
			lhs.copyState(U);
			return CHANGED;
		} else {
			return NOT_CHANGED;
		}
	}

}
