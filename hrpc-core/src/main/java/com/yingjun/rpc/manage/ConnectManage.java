package com.yingjun.rpc.manage;

import com.yingjun.rpc.client.RPCClientHandler;
import com.yingjun.rpc.codec.RPCDecoder;
import com.yingjun.rpc.codec.RPCEncoder;
import com.yingjun.rpc.protocol.RPCRequest;
import com.yingjun.rpc.protocol.RPCResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 管理所有的服务连接
 *
 * @author yingjun
 */
public class ConnectManage {

    private static final Logger logger = LoggerFactory.getLogger(ConnectManage.class);
    private volatile static ConnectManage connectManage;
    EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private CopyOnWriteArrayList<RPCClientHandler> connectedHandlerList = new CopyOnWriteArrayList<RPCClientHandler>();
    private Map<InetSocketAddress, RPCClientHandler> connectedHandlerMap = new ConcurrentHashMap<InetSocketAddress, RPCClientHandler>();
    private Map<String, SameInterfaceRPCHandlers> interfaceAndHandlersMap = new ConcurrentHashMap<String, SameInterfaceRPCHandlers>();

    private final int reconnectTime = 5000;//重连时间
    private final int connecntTimeOut = 6000;//连接超时时间

    //线程控制
    private CountDownLatch countDownLatch;
    //可重入锁
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();

    protected long connectTimeoutMillis = 6000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRuning = true;

    private ConnectManage() {
    }

    public static ConnectManage getInstance() {
        if (connectManage == null) {
            synchronized (ConnectManage.class) {
                if (connectManage == null) {
                    connectManage = new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    /**
     * 服务更新操作
     *
     * @param newServerAddress
     * @param interfaceAndServerMap
     */
    public void updateConnectedServer(Set<String> newServerAddress,
                                                   Map<String, Set<InetSocketAddress>> interfaceAndServerMap) {

        if (newServerAddress != null) {

            //整理出需要连接的服务地址
            Set<InetSocketAddress> newServerNodeSet = new HashSet<InetSocketAddress>();
            for (String newServerAddres : newServerAddress) {
                String[] array = newServerAddres.split(":");
                if (array.length == 2) {
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    InetSocketAddress socketAddress = new InetSocketAddress(host, port);
                    newServerNodeSet.add(socketAddress);
                }
            }

            //删除无效的服务
            for (int i = 0; i < connectedHandlerList.size(); i++) {
                RPCClientHandler handler = connectedHandlerList.get(i);
                InetSocketAddress socketAddress = handler.getSocketAddress();
                if (!newServerNodeSet.contains(socketAddress)) {
                    logger.info("remove and close invalid server node: " + socketAddress);
                    handler.close();
                    connectedHandlerList.remove(handler);
                    connectedHandlerMap.remove(socketAddress);
                }
            }

            //若发现新的未创建连接的服务，则去创建连接
            int needToConnectNum = 0;
            for (InetSocketAddress serverNodeAddress : newServerNodeSet) {
                RPCClientHandler handler = connectedHandlerMap.get(serverNodeAddress);
                if (handler == null) {
                    needToConnectNum++;
                }
            }
            if (needToConnectNum > 0) {
                countDownLatch = new CountDownLatch(needToConnectNum);
                for (InetSocketAddress serverNodeAddress : newServerNodeSet) {
                    RPCClientHandler handler = connectedHandlerMap.get(serverNodeAddress);
                    if (handler == null) {
                        connectServerNode(serverNodeAddress);
                    }
                }
            }
            try {
                if(countDownLatch!=null){
                    countDownLatch.await(connecntTimeOut, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            //更新 interfaceAndHandlersMap
            for (String key : interfaceAndServerMap.keySet()) {
                SameInterfaceRPCHandlers handlers = new SameInterfaceRPCHandlers();
                Set<InetSocketAddress> set = interfaceAndServerMap.get(key);
                for (InetSocketAddress inetSocketAddress : set) {
                    RPCClientHandler handler = connectedHandlerMap.get(inetSocketAddress);
                    if (handler != null) {
                        handlers.addHandler(handler);
                    }
                }
                interfaceAndHandlersMap.put(key, handlers);
            }

            logger.info("current connectedHandlerList: {}", connectedHandlerList);
            logger.info("current connectedHandlerMap: {}", connectedHandlerMap);
            logger.info("current interfaceAndHandlersMap: {}", interfaceAndHandlersMap);


        } else {
            logger.error("no available server node. all server nodes are down !!!");
            for (RPCClientHandler handler : connectedHandlerList) {
                logger.info("remove invalid server node: " + handler.getSocketAddress());
                handler.close();//关闭和服务器的连接
            }
            connectedHandlerList.clear();
            connectedHandlerMap.clear();
            interfaceAndHandlersMap.clear();
            logger.error("connectedHandlerList connectedHandlerMap interfaceAndHandlersMap has bean cleared!!!");
        }
    }


    /**
     * 创建各个服务的连接（基于netty）
     *
     * @param remote
     */
    private void connectServerNode(final InetSocketAddress remote) {
        logger.info("start connect to remote server: {}", remote);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connecntTimeOut)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline cp = socketChannel.pipeline();
                        cp.addLast(new RPCEncoder(RPCRequest.class));
                        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
                        cp.addLast(new RPCDecoder(RPCResponse.class));
                        cp.addLast(new RPCClientHandler() {
                            @Override
                            public void handlerCallback(RPCClientHandler handler, boolean isActive) {
                                if (isActive) {
                                    logger.info("Active: " + handler.getSocketAddress());
                                    connectedHandlerList.add(handler);
                                    connectedHandlerMap.put(handler.getSocketAddress(), handler);
                                    countDownLatch.countDown();
                                } else {
                                    logger.error("Inactive: " + handler.getSocketAddress());
                                }
                            }
                        });
                    }
                });
        bootstrap.connect(remote).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    logger.info("success connect to remote server: {}", remote);
                } else {
                    //不停的重连
                    logger.info("failed connect to remote server: {} will reconnect {} millseconds later", remote, reconnectTime);
                    channelFuture.channel().eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            connectServerNode(remote);
                        }
                    }, reconnectTime, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    public RPCClientHandler chooseHandler(String face) {
        SameInterfaceRPCHandlers handlers = interfaceAndHandlersMap.get(face);
        if (handlers != null) {
            return handlers.getSLBHandler();
        } else {
            return null;
        }

    }

    public void stop() {
        isRuning = false;
        for (int i = 0; i < connectedHandlerList.size(); ++i) {
            RPCClientHandler handler = connectedHandlerList.get(i);
            handler.close();
        }
        eventLoopGroup.shutdownGracefully();
    }
}
