package com.quincy.sdk;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.util.Assert;
/**
 * Twitter_Snowflake<br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
 * 加起来刚好64位，为一个Long型。<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 */
public class SnowFlakeAlgorithm {
	private static Random random = new Random();
	private final static Map<Integer, int[]> SHARDING_KEY_LENGTH_RANGE_MAP = new HashMap<Integer, int[]>(4);

	public static synchronized int generateShardingKeyValue(int length) {
		int[] lowerAndUppder = SHARDING_KEY_LENGTH_RANGE_MAP.get(length);
		Assert.notNull(lowerAndUppder, "Length must in 1, 2, 3, 4, 6.");
		return random.nextInt(lowerAndUppder[0], lowerAndUppder[1]);
	}

	// 初始时间戳(纪年)，可用雪花算法服务上线时间戳的值
    // 1650789964886：2022-04-24 16:45:59
//	private static final long INIT_EPOCH = 1650789964886L;
    private static long INIT_EPOCH = 1724222448348L;
    // 时间位取&
    private static final long TIME_BIT = 0b1111111111111111111111111111111111111111110000000000000000000000L;
    // 记录最后使用的毫秒时间戳，主要用于判断是否同一毫秒，以及用于服务器时钟回拨判断
    private long lastTimeMillis = -1L;
    // dataCenterId占用的位数
    private static long DATA_CENTER_ID_BITS = 5L;
    // dataCenterId占用5个比特位，最大值31
    // 0000000000000000000000000000000000000000000000000000000000011111
    private static long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    // workId占用的位数
    private static final long WORKER_ID_BITS = 5L;
    // workId占用5个比特位，最大值31
    // 0000000000000000000000000000000000000000000000000000000000011111
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 最后12位，代表每毫秒内可产生最大序列号，即 2^12 - 1 = 4095
    private static long SEQUENCE_BITS = 12L;
    // 掩码（最低12位为1，高位都为0），主要用于与自增后的序列号进行位与，如果值为0，则代表自增后的序列号超过了4095
    // 0000000000000000000000000000000000000000000000000000111111111111
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    // 同一毫秒内的最新序号，最大值可为 2^12 - 1 = 4095
    private long sequence;
    // workId位需要左移的位数 12
    private static long WORK_ID_SHIFT = SEQUENCE_BITS;
    // dataCenterId位需要左移的位数 12+5
    private static long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳需要左移的位数 12+5+5
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
//    private static long dataCenterId = 1;
    private static long workerId = 1;

    static {
		SHARDING_KEY_LENGTH_RANGE_MAP.put(1, new int[] {0, 7, 3});
		SHARDING_KEY_LENGTH_RANGE_MAP.put(2, new int[] {16, 63, 6});
		SHARDING_KEY_LENGTH_RANGE_MAP.put(3, new int[] {128, 511, 9});
		SHARDING_KEY_LENGTH_RANGE_MAP.put(4, new int[] {1024, 8191, 13});
		SHARDING_KEY_LENGTH_RANGE_MAP.put(6, new int[] {131072, 524287, 19});
		SEQUENCE_BITS -= 6;
		WORK_ID_SHIFT = SEQUENCE_BITS;
		DATA_CENTER_ID_BITS += 6;
		DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
		MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
		System.out.println("SEQUENCE_BITS---"+SEQUENCE_BITS);
		System.out.println("DATA_CENTER_ID_BITS---"+DATA_CENTER_ID_BITS);
		System.out.println("DATA_CENTER_ID_SHIFT---"+DATA_CENTER_ID_SHIFT);
		System.out.println("MAX_DATA_CENTER_ID---"+MAX_DATA_CENTER_ID);
		System.out.println("TIMESTAMP_SHIFT---"+TIMESTAMP_SHIFT);
	}

	/*public static void setDataCenterId(long dataCenterId) {
		if(dataCenterId<0||dataCenterId>MAX_DATA_CENTER_ID)
            throw new IllegalArgumentException(String.format("Datacenter Id can't be greater than %d or less than 0.", MAX_DATA_CENTER_ID));
		SnowFlakeAlgorithm.dataCenterId = dataCenterId;
	}*/
	public static void setWorkerId(long workerId) {
		if(workerId<0||workerId>MAX_WORKER_ID)
            throw new IllegalArgumentException(String.format("Worker Id can't be greater than %d or less than 0.", MAX_WORKER_ID));
		SnowFlakeAlgorithm.workerId = workerId;
	}

	private static SnowFlakeAlgorithm snowFlakeUtil = new SnowFlakeAlgorithm();

	private SnowFlakeAlgorithm() {}
	public static SnowFlakeAlgorithm getObject() {
		return snowFlakeUtil;
	}
    /**
     * 获取唯一ID
     * @return
     */
    public static Long nextId() {
        return snowFlakeUtil.nextId(random.nextInt(1024, 2047));
    }
    /**
     * 通过雪花算法生成下一个id，注意这里使用synchronized同步
     * @return 唯一id
     */
    public synchronized long nextId(int shardingKey) {
    	System.out.println("shardingKey=================="+shardingKey);
        long currentTimeMillis = System.currentTimeMillis();
        // 当前时间小于上一次生成id使用的时间，可能出现服务器时钟回拨问题
        if(currentTimeMillis < lastTimeMillis)
            throw new RuntimeException(String.format("可能出现服务器时钟回拨问题，请检查服务器时间。当前服务器时间戳：%d，上一次使用时间戳：%d", currentTimeMillis, lastTimeMillis));
        if(currentTimeMillis==lastTimeMillis) {
            // 还是在同一毫秒内，则将序列号递增1，序列号最大值为4095
            // 序列号的最大值是4095，使用掩码（最低12位为1，高位都为0）进行位与运行后如果值为0，则自增后的序列号超过了4095
            // 那么就使用新的时间戳
            sequence = (sequence+1)&SEQUENCE_MASK;
            if(sequence==0)
                currentTimeMillis = getNextMillis(lastTimeMillis);
        } else //不在同一毫秒内，则序列号重新从0开始，序列号最大值为4095
            sequence = 0;
        // 记录最后一次使用的毫秒时间戳
        lastTimeMillis = currentTimeMillis;
        // 核心算法，将不同部分的数值移动到指定的位置，然后进行或运行
        // <<：左移运算符, 1 << 2 即将二进制的 1 扩大 2^2 倍
        // |：位或运算符, 是把某两个数中, 只要其中一个的某一位为1, 则结果的该位就为1
        // 优先级：<< > |
        long id = ((currentTimeMillis - INIT_EPOCH) << TIMESTAMP_SHIFT)//时间戳部分
//        		| (dataCenterId << DATA_CENTER_ID_SHIFT)//数据中心部分
        		| (shardingKey << DATA_CENTER_ID_SHIFT)//路由键代替数据中心部分
        		| (workerId << WORK_ID_SHIFT)//机器表示部分
        		| sequence;//序列号部分
        long extectedShardingKey = extractShardingKey(id);
        if(extectedShardingKey!=shardingKey) {
        	System.err.println("SHARDING_KEY不符："+id+"---"+shardingKey+"---"+extectedShardingKey+"---"+currentTimeMillis+"---"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(currentTimeMillis));
        }
        return id;
    }
    /**
     * 获取指定时间戳的接下来的时间戳，也可以说是下一毫秒
     * @param lastTimeMillis 指定毫秒时间戳
     * @return 时间戳
     */
    private long getNextMillis(long lastTimeMillis) {
        long currentTimeMillis = System.currentTimeMillis();
        while (currentTimeMillis <= lastTimeMillis) {
            currentTimeMillis = System.currentTimeMillis();
        }
        return currentTimeMillis;
    }
    /**
     * 获取随机字符串，length=13
     */
    public static String getRandomStr() {
        return Long.toString(nextId(), Character.MAX_RADIX);
    }
    /**
     * 从ID中获取时间
     * @param id 由此类生成的ID
     */
    public static long extractTime(long id) {
        return ((TIME_BIT & id) >> TIMESTAMP_SHIFT) + INIT_EPOCH;
    }
    /**
     * 截取Sharding Key的掩码
     */
//    private static final long SHARDING_KEY_MASK = 0b0000000000000000000000000000000000000000001111111111100000000000L;
    private static final long SHARDING_KEY_MASK = (2048-1)<<11;

    public static long extractShardingKey(long id) {
    	return ((id&SHARDING_KEY_MASK)>>DATA_CENTER_ID_SHIFT);
    }
 
    public static void main(String[] args) throws ParseException {
//    	System.out.println(System.currentTimeMillis()+"=========="+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1924-04-23 11:23:45").getTime());
//    	System.out.println(Integer.MAX_VALUE+"---"+String.valueOf(Integer.MAX_VALUE).length());
//		System.out.println(Long.MAX_VALUE+"---"+String.valueOf(Long.MAX_VALUE).length());
//		System.out.println(Math.pow(2, 15)+Math.pow(2, 14)+Math.pow(2, 13)+Math.pow(2, 12)+Math.pow(2, 11)+Math.pow(2, 10)+Math.pow(2, 9)+Math.pow(2, 8));
//		System.out.println(Math.pow(2, 15)+Math.pow(2, 14)+Math.pow(2, 13)+Math.pow(2, 12)+Math.pow(2, 10)+Math.pow(2, 9)+Math.pow(2, 8)+255);
//		System.out.println(new String(new byte[]{53, 46, 54, 46, 51, 48}));
		/*Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < 200; i++) {
			int shardingKey = SnowFlakeAlgorithm.generateShardingKeyValue(4);
			int remainder = shardingKey%8;
			Integer count = map.get(remainder);
			map.put(remainder, (count==null?0:count)+1);
		}
		for(Entry<Integer, Integer> e:map.entrySet()) {
			System.out.println(e.getKey()+"-------"+e.getValue());
		}*/
        /*for (int i = 0; i < 1; i++) {
        	long id = SnowFlakeAlgorithm.nextId();
            System.out.println("id: "+id+"---"+String.valueOf(id).length());
            Date date = SnowFlakeAlgorithm.getTimeBySnowFlakeId(id);
            System.out.println("date: "+date);
            long time = date.getTime();
            System.out.println("time: "+time);
            System.out.println("RandomStr: "+getRandomStr());
            System.out.println("--------------------------------"+SnowFlakeAlgorithm.getObject().hashCode());
        }*/
//        System.out.println(Math.pow(2, 22));
    	for (int i = 0; i < 10; i++) {
    		Long id = SnowFlakeAlgorithm.nextId();
    		System.out.println(id+"-----"+SnowFlakeAlgorithm.extractShardingKey(id));
    	}
    }
}