package com.quincy.core;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.springframework.beans.factory.annotation.Value;

import com.quincy.sdk.SnowFlake;

public class ZKServerIdConfiguration {
	private CuratorFramework zkClient;
	@Value("${zk.connectionString}")
	private String connectionString;
	private static final String ROOT_PATH = "/server-id-manager";

	@PostConstruct
	public void init() throws Exception {
//		zkClient = CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(1000, 3));
		zkClient = CuratorFrameworkFactory.builder()
		        .connectString(connectionString)
		        .sessionTimeoutMs(60 * 1000)      // 会话超时时间ZK服务端默认30s，建议不要超过30s
		        .connectionTimeoutMs(15 * 1000)   // 建立连接超时
		        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
		        .namespace("myapp")               // 设置命名空间，所有操作根路径 /myapp，隔离不同业务
		        .build();
		/*zkClient.getConnectionStateListenable().addListener((curatorClient, newState) -> {
		    System.out.println("ZK连接状态变更：" + newState);
		    if(newState.isConnected()) {
		        System.out.println("连接建立成功");
		    }
		    if(newState == ConnectionState.LOST) {
		        //会话丢失，会话过期，需要处理
		    }
		});*/
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			zkClient.close();
		}));
		zkClient.start();
		initIdNodes();
		SnowFlake.setWorkerId(acquireServerId());
	}

	private void initIdNodes() throws Exception {
		for(int id = 0; id <= SnowFlake.MAX_WORKER_ID; id++) {
            String path = ROOT_PATH + "/" + id;
            try {
            	 if(zkClient.checkExists().forPath(path) == null) {
            		 zkClient.create()
                             .creatingParentsIfNeeded()
                             .withMode(CreateMode.PERSISTENT)
                             .forPath(path);
                 }
            } catch (NodeExistsException e) {
				continue;
			}
        }
	}

	private int acquireServerId() throws Exception {
		for(int candidateId = 0; candidateId <= SnowFlake.MAX_WORKER_ID; candidateId++) {
			String idParentPath = ROOT_PATH + "/" + candidateId;
			// 查看该ID下是否有子节点，有=被占用
			List<String> children = zkClient.getChildren().forPath(idParentPath);
			if(children != null && !children.isEmpty()) {
				continue; // ID已占用，下一个
			}
			// 尝试创建临时子节点，抢占ID
			String epPath = idParentPath + "/occupied";
			try {
				zkClient.create().withMode(CreateMode.EPHEMERAL) //重点：临时节点
				.forPath(epPath);
				// 创建成功，抢占成功
				return candidateId;
			} catch (NodeExistsException e) {
				// 并发冲突，别的服务抢先一步，继续循环
				continue;
			}
		}
		// 0~31全部占满
		throw new RuntimeException("没有可用serverId，0~31全部被占用");
	}

	@PreDestroy
    public void destroy() {
		zkClient.close();
    }
}
