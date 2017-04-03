package net.flyingff.bsbridge;

import net.flyingff.bsbridge.ImageFinder.GaussianFilterLib;

public class GaussianFilterLibFFImpl implements GaussianFilterLib {
	private static final int ToByte(float f) {
		return f > 255? 255 : (int)f;
	}
	@Override
	public void gaussian(int[] dataIn, int[] dataOut, int rows, int cols, float[] serial, int serialSize) {
		int halfSerialSize = serialSize / 2;
		float sum[] = new float[4];

		// for each row
		for (int row = 0; row < rows; row++) {
			for (int i = 0; i < cols; i++) {
				sum[0] = sum[1] = sum[2] = sum[3] = 0;
				for (int j = -halfSerialSize, k = 0; j <= halfSerialSize; j++, k++) {
					int index = i + j;
					if (index < 0) {
						index = 0; 
					} else if(index >= cols) {
						index = cols - 1;
					}
					int ptr = dataIn[row * cols + index];
					float sval = serial[k];
					sum[0] += sval * (ptr & 0xFF);
					sum[1] += sval * ((ptr >> 8) & 0xFF);
					sum[2] += sval * ((ptr >> 16) & 0xFF);
					//sum[3] += sval * ((ptr >> 8) & 0xFF);
				}
				dataOut[row * cols + i] = (dataOut[row * cols + i] & 0xFF000000)
						| ToByte (sum[0])
						| (ToByte (sum[1]) << 8)
						| (ToByte (sum[2]) << 16);
			}
		}
		
		// for each column
		for (int col = 0; col < cols; col ++) {
			for (int i = 0; i < rows; i++) {
				sum[0] = sum[1] = sum[2] = sum[3] = 0;
				for (int j = -halfSerialSize, k = 0; j <= halfSerialSize; j++, k++) {
					int index = i + j;
					if (index < 0) {
						index = 0;
					}
					else if (index >= rows) {
						index = rows - 1;
					}
					int ptr = dataOut[cols * index + col];
					float sval = serial[k];
					sum[0] += sval * (ptr & 0xFF);
					sum[1] += sval * ((ptr >> 8) & 0xFF);
					sum[2] += sval * ((ptr >> 16) & 0xFF);
				}
				dataIn[cols * i + col] = (dataIn[cols * i + col] & 0xFF000000)
						| ToByte (sum[0])
						| (ToByte (sum[1]) << 8)
						| (ToByte (sum[2]) << 16);
			}
		}
	}

	
	private static final int adiff(int X, int Y) {
		return ((X) > (Y)? (X) - (Y) : (Y) - (X));
	}
	private static final int picpos(int COLS, int R, int C) {
		return (((R) * (COLS) + (C)));
	}
	@Override
	public int find(int[] dataBig, int rowsBig, int colsBig, int[] dataSmall, int rowsSmall, int colsSmall,
			int threashold) {
		int rowSpace = rowsBig - rowsSmall, colSpace = colsBig - colsSmall;
		int centerR = rowsSmall / 2, centerC = colsSmall / 2;

		int samplePtSmall[] = {
			picpos (colsSmall, 1, 1),
			picpos (colsSmall, rowsSmall - 2, 1),
			picpos (colsSmall, 1, colsSmall - 2),
			picpos (colsSmall, rowsSmall - 2, colsSmall - 2),
			picpos (colsSmall, centerR, centerC),
		};
		int samplePtBig[] = {
			picpos (colsBig, 1, 1),
			picpos (colsBig, rowsSmall - 2, 1),
			picpos (colsBig, 1, colsSmall - 2),
			picpos (colsBig, rowsSmall - 2, colsSmall - 2),
			picpos (colsBig, centerR, centerC),
		};

		int diff;

		for (int r = 0; r < rowSpace; r++) {
			for (int c = 0; c < colSpace; c++) {
				int maxDiff = 0;

				// firstly test 5 points
				for (int k = 0; k < 5; k++) {
					int p1 = dataSmall[samplePtSmall[k]], p2 = dataBig[samplePtBig[k] + (r * colsBig + c)];
					diff = adiff (p1 & 0xFF, p2 & 0xFF);
					if (diff > maxDiff) { maxDiff = diff; }
					p1>>>= 8; p2>>>= 8;
					diff = adiff (p1 & 0xFF, p2 & 0xFF);
					if (diff > maxDiff) { maxDiff = diff; }
					p1>>>= 8; p2>>>= 8;
					diff = adiff (p1 & 0xFF, p2 & 0xFF);
					if (diff > maxDiff) { maxDiff = diff; }
				}
				if (maxDiff > threashold) {
					continue;
				} else {
					for (int rs = 0; rs < rowsSmall; rs++) {
						int p1 = rs * colsSmall;
						int p2 = (rs + r) * colsBig + c;

						for (int cs = 0; cs < colsSmall; cs++) {
							int v1 = dataSmall[p1], v2 = dataBig[p2];
							diff = adiff (v1 & 0xFF, v2 & 0xFF);
							if (diff > maxDiff) maxDiff = diff;
							v1 >>>= 8; v2 >>>= 8;
							diff = adiff (v1 & 0xFF, v2 & 0xFF);
							if (diff > maxDiff) maxDiff = diff;
							v1 >>>= 8; v2 >>>= 8;
							diff = adiff (v1 & 0xFF, v2 & 0xFF);
							if (diff > maxDiff) maxDiff = diff;
							
							p1++; p2++;
						}

						if (maxDiff > threashold) {
							break;
						}
					}
				}
				// found
				if (maxDiff <= threashold) {
					// printf ("Pt found: (%d, %d), %d\n", c, r, maxDiff);
					return (r << 16) | (c & 0xFFFF);
				}
			}
		}

		// not found
		return -1;
	}
}
