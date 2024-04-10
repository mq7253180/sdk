package com.quincy.sdk.helper;

public class ArabicFiguresToChinese {
	private final static int[] BIG_DIVISORS = {100000000, 10000, 1};
	private final static String[] BIG_UNITS = {"亿", "万", ""};
	private final static int[] DIVISORS = {1000, 100, 10, 1};
	private final static String[] UNITS = {"仟", "佰", "拾", ""};
	private final static String[] CHN_DIGITS = {"", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};

	public static String transfer(int from) {
		StringBuilder sb = new StringBuilder(20);
		boolean previousIsNot0 = true;
		int p = from;
		for(int i=0;i<BIG_DIVISORS.length;i++) {
			int bigDivisor = BIG_DIVISORS[i];
			if(p<bigDivisor)
				continue;
			int deductor = 0;
			int x = p/bigDivisor;
			p = p%bigDivisor;
			if(x<1000&&sb.length()>0)
				sb.append("零");
			for(int j=0;j<DIVISORS.length;j++) {
				int divisor = DIVISORS[j];
				if(x<divisor)
					continue;
				int t = x-deductor;
				int r = t/DIVISORS[j];
				if(r==0) {
					if(j<DIVISORS.length-1&&previousIsNot0) {//不是个位&&前一位不是零
						sb.append("零");
						previousIsNot0 = false;
					}
				} else {
					sb.append(CHN_DIGITS[r]).append(UNITS[j]);
					previousIsNot0 = true;
				}
				deductor += divisor*r;
			}
			sb.append(BIG_UNITS[i]);
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		System.out.println(transfer(40));
		System.out.println(transfer(50040));
		System.out.println(transfer(1005));
		System.out.println(transfer(20008));
		System.out.println(transfer(30078));
		System.out.println(transfer(20808));
		System.out.println(transfer(50031565));
		System.out.println("我国汉族人口总数："+transfer(1286311334));
	}
}