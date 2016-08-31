package com.yingjun.rpc.server;

import com.yingjun.rpc.annotation.HRPCService;
import com.yingjun.rpc.codec.RPCDecoder;
import com.yingjun.rpc.codec.RPCEncoder;
import com.yingjun.rpc.protocol.RPCRequest;
import com.yingjun.rpc.protocol.RPCResponse;
import com.yingjun.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RPC Server
 *
 * @author yingjun
 */
public class RPCServer implements BeanNameAware, BeanFactoryAware, ApplicationContextAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(RPCServer.class);
    private ServiceRegistry serviceRegistry;
    private String serverAddress;

    //存放服务名与服务对象之间的映射关系
    private Map<String, Object> serviceBeanMap = new ConcurrentHashMap<String, Object>();
    //采用线程池，提高接口调用性能
    private static ExecutorService threadPoolExecutor;

    //实例化
    public RPCServer(String serverAddress, String zookeeper) {
        this.serverAddress = serverAddress;
        serviceRegistry = new ServiceRegistry(zookeeper);
    }

    @Override
    public void setBeanName(String s) {
        logger.info("setBeanName() {}", s);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        logger.info("setBeanFactory()");
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        logger.info("setApplicationContext()");
        //扫描含有@RPCService的注解类
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(HRPCService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                //获取接口名称
                String interfaceName = serviceBean.getClass().getAnnotation(HRPCService.class).value().getName();
                logger.info("@HRPCService:" + interfaceName);
                //在zookeeper上注册该接口服务
                serviceRegistry.createInterfaceAddressNode(interfaceName, serverAddress);
                //本地保存该接口服务
                this.serviceBeanMap.put(interfaceName, serviceBean);
            }
        }
    }

    @Override
    //在实例被创建时执行，后续及是init-method
    //创建netty服务
    public void afterPropertiesSet() throws Exception {
        logger.info("afterPropertiesSet()");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                    .addLast(new RPCDecoder(RPCRequest.class))
                                    .addLast(new RPCEncoder(RPCResponse.class))
                                    .addLast(new RPCServerHandler(serviceBeanMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 通过NoDelay禁用Nagle,使消息立即发出去，不用等待到一定的数据量才发出去
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);

            ChannelFuture future = bootstrap.bind(host, port).sync();

            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void submit(Runnable task) {
        if (threadPoolExecutor == null) {
            synchronized (RPCServer.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = Executors.newFixedThreadPool(16);
                }
            }
        }
        threadPoolExecutor.submit(task);
    }

}
