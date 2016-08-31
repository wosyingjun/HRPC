package com.yingjun.rpc.server;

import com.yingjun.rpc.protocol.RPCRequest;
import com.yingjun.rpc.protocol.RPCResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * RPC request processor
 *
 * @author yingjun
 */
public class RPCServerHandler extends SimpleChannelInboundHandler<RPCRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RPCServerHandler.class);
    private final Map<String, Object> serviceBeanMap;

    public RPCServerHandler(Map<String, Object> serviceBeanMap) {
        this.serviceBeanMap = serviceBeanMap;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("======rpc server channel active：" + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("======rpc server channel inactive：" + ctx.channel().remoteAddress());
    }


    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RPCRequest request) throws Exception {
        logger.info("======rpc server channelRead0：" + ctx.channel().remoteAddress());
        RPCServer.submit(new Runnable() {
            @Override
            public void run() {
                logger.info("receive request:" + request.getRequestId() +
                        " className:" + request.getClassName() +
                        " methodName:" + request.getMethodName());
                RPCResponse response = new RPCResponse();
                response.setRequestId(request.getRequestId());
                try {
                    //通过反射原理找到对应的服务类和方法
                    String className = request.getClassName();
                    Object serviceBean = serviceBeanMap.get(className);

                    String methodName = request.getMethodName();
                    Class<?>[] parameterTypes = request.getParameterTypes();
                    Object[] parameters = request.getParameters();

                    // JDK reflect
                    /*Method method = serviceClass.getMethod(methodName, parameterTypes);
                    method.setAccessible(true);
                    Object result=method.invoke(serviceBean, parameters);*/

                    // 避免使用 Java 反射带来的性能问题，我们使用 CGLib 提供的反射 API
                    FastClass serviceFastClass = FastClass.create(serviceBean.getClass());
                    FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
                    Object result = serviceFastMethod.invoke(serviceBean, parameters);

                    response.setResult(result);
                } catch (Exception e) {
                    response.setError(e.getMessage());
                    logger.error("Exception", e);
                }
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.info("send response for request: " + request.getRequestId());
                    }
                });
            }
        });
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("rpc server caught exception: " + ctx.channel().remoteAddress() + "|" + cause.getMessage());
        ctx.close();
    }
}
