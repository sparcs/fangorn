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
package au.edu.unimelb.csse.bit;

public class ByteOps {
	private PosMid p = new PosMid();
	private int[] d = new int[4];

	/**
	 * the values are in the order right, left, depth, parent
	 * @param values
	 * @param bytes
	 * @return
	 */
	public PosMid int4ToDiffVarBytes(int[] values, byte[] bytes) {
		final Order order = Order.find(values[0], values[1], values[2],
				values[3]);
		order.diffs(values, d);
		int id = order.id;
		p.clear();
		p = intToVarByte(id, bytes, p);
		for (int i = 0; i < d.length; i++) {
			p = intToVarByte(d[i], bytes, p);
		}
		return p;
	}

	public PosMid int4ToVarBytes(int[] values, byte[] bytes) {
		p.clear();
		for (int i = 0; i < values.length; i++) {
			p = intToVarByte(values[i], bytes, p);
		}
		return p;
	}

	public void int4ToBytes(int[] values, byte[] bytes) {
		for (int i = 0; i < values.length; i++) {
			intToByte(values[i], bytes, i);
		}
	}

	private void intToByte(int i, byte[] bytes, int pos) {
		bytes[pos] = (byte) (i & 0xFF);
	}

	public static PosMid intToVarByte(int i, byte[] bytes, PosMid p) {
		if (i == 0) {
			if (p.mid) {
				bytes[p.position] = (byte) (bytes[p.position] & 0x0F);
				p.mid = false;
				p.position++;
			} else {
				bytes[p.position] = (byte) 0x00;
				p.mid = true;
			}
		}
		while (i > 0) {
			if (p.mid) {
				bytes[p.position] = (byte) ((bytes[p.position] & 0x0F) | (i & 0x7) << 4);
				i = i >>> 3;
				if (i > 0) {
					bytes[p.position] = (byte) (bytes[p.position] | 0x80);
				}
				p.mid = false;
				p.position++;
			} else {
				bytes[p.position] = (byte) (i & 0x7);
				i = i >>> 3;
				if (i > 0) {
					bytes[p.position] = (byte) (bytes[p.position] | 0x08);
				}
				p.mid = true;
			}
		}
		return p;
	}

	public static class PosMid {
		public int position;
		public boolean mid;

		void clear() {
			position = 0;
			mid = false;
		}
	}

	public static final Order DLRP = new Order(0, 2, 1, 0, 3);
	public static final Order LPRD = new Order(1, 1, 3, 0, 2);
	public static final Order LDRP = new Order(2, 1, 2, 0, 3);
	public static final Order DLPR = new Order(3, 2, 1, 3, 0);
	public static final Order LRPD = new Order(4, 1, 0, 3, 2);
	public static final Order LRDP = new Order(5, 1, 0, 2, 3);
	public static final Order LPDR = new Order(6, 1, 3, 2, 0);
	public static final Order LDPR = new Order(7, 1, 2, 3, 0);

	public static class Order {
		private final int id;
		private final int[] order = new int[4];

		public Order(int id, int first, int second, int third, int fourth) {
			this.id = id;
			order[0] = first;
			order[1] = second;
			order[2] = third;
			order[3] = fourth;
		}

		public void diffs(int[] values, int[] d) {
			d[0] = values[order[0]];
			d[1] = values[order[1]] - values[order[0]];
			d[2] = values[order[2]] - values[order[1]];
			d[3] = values[order[3]] - values[order[2]];
		}

		public static Order find(int r, int l, int d, int p) {
			if (l < d) {
				// l < d
				if (p < d) {
					// p cannot be < l, therefore l < p < d
					if (r < p) {
						// l < r < p < d
						return ByteOps.LRPD;
					} else {
						// l < p < d and r > p
						if (r < d) {
							// l < p < r < d
							return ByteOps.LPRD;
						} else {
							// l < p < d < r
							return ByteOps.LPDR;
						}
					}
				} else {
					// l < d < p
					if (p < r) {
						// l < d < p < r
						return ByteOps.LDPR;
					} else {
						// l < d < p and r < p
						if (r < d) {
							// l < r < d < p
							return ByteOps.LRDP;
						} else {
							// l < d < r < p
							return ByteOps.LDRP;
						}
					}
				}
			} else {
				// d < l
				if (r < p) {
					// d < l < r < p
					return ByteOps.DLRP;
				} else {
					// d < l < p < r
					return ByteOps.DLPR;
				}
			}
		}
	}

	static class OrderLookup {

		public static Order find(int id) {
			switch (id) {
			case 0:
				return DLRP;
			case 1:
				return LPRD;
			case 2:
				return LDRP;
			case 3:
				return DLPR;
			case 4:
				return LRPD;
			case 5:
				return LRDP;
			case 6:
				return LPDR;
			case 7:
				return LDPR;
			}
			return null;
		}
	}

	public static void convertToInts(byte[] bytes, int[] ints, int intstart) {
		int prev = 0;
		int numberOfInts = 0;
		int moves = 0;
		Order o = null;
		for (int i = 0; i < bytes.length && numberOfInts < 5; i++) {
			byte b = bytes[i];
			prev = prev | (b & 0x07) << (moves * 3);
			moves++;
			if ((b & 0x08) == 0) {
				o = extracted(ints, intstart, prev, numberOfInts, o);
				prev = 0;
				moves = 0;
				numberOfInts++;
			}
			if (numberOfInts == 5)
				break;
			prev = prev | (((b & 0x70) >>> 4) << (moves * 3));
			moves++;
			if ((b & 0x80) == 0) {
				o = extracted(ints, intstart, prev, numberOfInts, o);
				prev = 0;
				moves = 0;
				numberOfInts++;
			}
		}
	}

	private static Order extracted(int[] ints, int intstart, int prev,
			int numberOfInts, Order o) {
		if (o == null) {
			o = OrderLookup.find(prev);
		} else {
			if (numberOfInts > 1) {
				ints[intstart + o.order[numberOfInts - 1]] = prev
						+ ints[intstart + o.order[numberOfInts - 2]];
			} else {
				ints[intstart + o.order[numberOfInts - 1]] = prev;
			}
		}
		return o;
	}
}
