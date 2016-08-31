package com.yingjun.rpc.registry;

import com.yingjun.rpc.utils.Config;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * RPC服务注册中心
 *
 * @author yingjun
 */
public class ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private CountDownLatch latch = new CountDownLatch(1);
    private String address;//注册中心地址
    private ZooKeeper zooKeeper;

    public ServiceRegistry(String address) {
        this.address = address;
        //连接zookeeper
        zooKeeper = connectServer();
        //创建根节点
        if (zooKeeper != null) {
            setRootNode();
        }
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(address, Config.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (IOException e) {
            logger.error("", e);
        } catch (InterruptedException ex) {
            logger.error("", ex);
        }
        return zk;
    }

    /**
     * 添加根节点
     */
    private void setRootNode() {
        try {
            Stat s = zooKeeper.exists(Config.ZK_ROOT_PATH, false);
            if (s == null) {
                //创建持久化目录节点，这个目录节点存储的数据不会丢失
                String path = zooKeeper.create(Config.ZK_ROOT_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.info("create zookeeper root node (path:{})", path);
            }
        } catch (KeeperException e) {
            logger.error(e.toString());
        } catch (InterruptedException e) {
            logger.error(e.toString());
        }
    }

    /**
     * 创建服务接口节点
     *
     * @param interfaceName
     */
    private void createInterfaceNode(String interfaceName) {
        try {
            String path = zooKeeper.create(Config.ZK_ROOT_PATH + "/" + interfaceName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            logger.info("create zookeeper interface node (path:{})", path);
        } catch (KeeperException e) {
            logger.error("", e);
        } catch (InterruptedException ex) {
            logger.error("", ex);
        }
    }

    /**
     * 创建服务接口地址节点
     * PERSISTENT：创建后只要不删就永久存在
     * EPHEMERAL：会话结束年结点自动被删除，EPHEMERAL结点不允许有子节点
     * SEQUENTIAL：节点名末尾会自动追加一个10位数的单调递增的序号，同一个节点的所有子节点序号是单调递增的
     * PERSISTENT_SEQUENTIAL：结合PERSISTENT和SEQUENTIAL
     * EPHEMERAL_SEQUENTIAL：结合EPHEMERAL和SEQUENTIAL
     *
     * @param interfaceName
     * @param serverAddress
     */
    public void createInterfaceAddressNode(String interfaceName, String serverAddress) {
        try {
            Stat s = zooKeeper.exists(Config.ZK_ROOT_PATH + "/" + interfaceName, false);
            if (s == null) {
                createInterfaceNode(interfaceName);
            }
            String path = zooKeeper.create(Config.ZK_ROOT_PATH + "/" + interfaceName + "/" + serverAddress,
                    serverAddress.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            logger.info("create zookeeper interface address node (path:{})", path);
        } catch (KeeperException e) {
            logger.error("", e);
        } catch (InterruptedException ex) {
            logger.error("", ex);
        }
    }


}