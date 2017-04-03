#include <stdio.h>
#include <math.h>

#define ToByte(F) (F > 255? 0xFF: (unsigned char)F)
extern "C" {
	__declspec(dllexport) void gaussian (int* dataIn, int* dataOut, int rows, int cols, float* serial, int serialSize) {
		
		int halfSerialSize = serialSize / 2;
		float sum[4] = { 0 };
		unsigned char* ptr;

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
					ptr = (unsigned char*)(dataIn + row * cols + index);
					float sval = serial[k];
					sum[0] += sval * ptr[0];
					sum[1] += sval * ptr[1];
					sum[2] += sval * ptr[2];
					sum[3] += sval * ptr[3];
				}
				ptr = (unsigned char*)(dataOut + row * cols + i);
				ptr[0] = ToByte (sum[0]);
				ptr[1] = ToByte (sum[1]);
				ptr[2] = ToByte (sum[2]);
				//ptr[3] = ToByte (sum[3]);
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
					ptr = (unsigned char*)(dataOut + cols * index + col);
					float sval = serial[k];
					sum[0] += sval * ptr[0];
					sum[1] += sval * ptr[1];
					sum[2] += sval * ptr[2];
					sum[3] += sval * ptr[3];
				}
				ptr = (unsigned char*)(dataIn + cols * i + col);
				ptr[0] = ToByte (sum[0]);
				ptr[1] = ToByte (sum[1]);
				ptr[2] = ToByte (sum[2]);
				//ptr[3] = ToByte (sum[3]);
			}
		}
		
	}

#define adiff(X, Y) ((X) > (Y)? (X) - (Y) : (Y) - (X))
#define picpos(SRC, COLS, R, C) ((SRC) + ((R) * (COLS) + (C)))
	typedef unsigned char u8;
	__declspec(dllexport) int find(int* dataBig, int rowsBig, int colsBig, int* dataSmall, int rowsSmall, int colsSmall, int threashold) {
		int rowSpace = rowsBig - rowsSmall, colSpace = colsBig - colsSmall;
		int centerR = rowsSmall / 2, centerC = colsSmall / 2;

		u8* samplePtSmall[] = {
			(u8*)picpos (dataSmall, colsSmall, 1, 1),
			(u8*)picpos (dataSmall, colsSmall, rowsSmall - 2, 1),
			(u8*)picpos (dataSmall, colsSmall, 1, colsSmall - 2),
			(u8*)picpos (dataSmall, colsSmall, rowsSmall - 2, colsSmall - 2),
			(u8*)picpos (dataSmall, colsSmall, centerR, centerC),
		};
		u8* samplePtBig[] = {
			(u8*)picpos (dataBig, colsBig, 1, 1),
			(u8*)picpos (dataBig, colsBig, rowsSmall - 2, 1),
			(u8*)picpos (dataBig, colsBig, 1, colsSmall - 2),
			(u8*)picpos (dataBig, colsBig, rowsSmall - 2, colsSmall - 2),
			(u8*)picpos (dataBig, colsBig, centerR, centerC),
		};
		
		// printf ("param=big[%dx%d],small[%dx%d],th=%d\n", colsBig, rowsBig, colsSmall, rowsSmall, threashold);

		u8 *p1, *p2;
		int diff;

		for (int r = 0; r < rowSpace; r++) {
			for (int c = 0; c < colSpace; c++) {
				int maxDiff = 0;

				// firstly test 5 points
				for (int k = 0; k < 5; k++) {
					p1 = samplePtSmall[k], p2 = samplePtBig[k] + ((r * colsBig + c) << 2);
					diff = adiff (*p1, *p2);
					if (diff > maxDiff) { maxDiff = diff; }
					p1++, p2++;
					diff = adiff (*p1, *p2);
					if (diff > maxDiff) { maxDiff = diff; }
					p1++, p2++;
					diff = adiff (*p1, *p2);
					if (diff > maxDiff) { maxDiff = diff; }
				}
				if (maxDiff > threashold) {
					continue;
				} else {
					// printf ("Pt considered: (%d, %d), %d\n", c, r, maxDiff);
					// fflush (stdout);
					for (int rs = 0; rs < rowsSmall; rs++) {
						p1 = (u8*)(dataSmall + rs * colsSmall);
						p2 = (u8*)(dataBig + (rs + r) * colsBig + c);

						for (int cs = 0; cs < colsSmall; cs++) {
							diff = adiff (*p1, *p2);
							if (diff > maxDiff) maxDiff = diff;
							p1++, p2++;
							diff = adiff (*p1, *p2);
							if (diff > maxDiff) maxDiff = diff;
							p1++, p2++;
							diff = adiff (*p1, *p2);
							if (diff > maxDiff) maxDiff = diff;
							p1 += 2; p2 += 2;
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

