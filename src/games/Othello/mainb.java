package Othello;

import java.util.Arrays;

public class mainb {

	public static void main(String[] args)
	{
		
		int[] as = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8 , 9, 10, 11, 12, 13, 14, 15 , 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 , 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 , 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
		
		int[] z = flip(as);
		System.out.println(Arrays.toString(z));
		System.out.println(Arrays.toString(flip(z)));
		
	}
	
	public static int[] flip(int[] table)
	{
		int ri[] = new int[] {56,48,40,32,24,16,8,00,57,49,41,33,25,17,9,1,58,50,42,34,26,18,10,2,59,51,43,35,27,19,11,3,60,52,44,36,28,20,12,4,61,53,45,37,29,21,13,5,62,54,46,38,30,22,14,6,63,55,47,39,31,23,15,7};
		int[] nb = new int[64];
		for (int k=0; k<64; k++) nb[k] =table[ri[k]];
		return nb;
	
	}
	
}
