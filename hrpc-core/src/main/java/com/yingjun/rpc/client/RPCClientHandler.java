package com.yingjun.rpc.client;

import com.yingjun.rpc.protocol.RPCRequest;
import com.yingjun.rpc.protocol.RPCResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client  handler
 *
 * @author yingjun
 */
public abstract class RPCClientHandler extends SimpleChannelInboundHandler<RPCResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RPCClientHandler.class);
    private ConcurrentHashMap<String, RPCFuture> pending = new ConcurrentHashMap<String, RPCFuture>();

    private volatile Channel channel;
    private InetSocketAddress socketAddress;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channel = ctx.channel();
        socketAddress = (InetSocketAddress) channel.remoteAddress();
        handlerCallback(channel.pipeline().get(RPCClientHandler.class), true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        handlerCallback(channel.pipeline().get(RPCClientHandler.class), false);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RPCResponse response) throws Exception {
        String requestId = response.getRequestId();
        RPCFuture rpcFuture = pending.get(requestId);
        if (rpcFuture != null) {
            pending.remove(requestId);
            rpcFuture.done(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("client caught exception", cause);
        ctx.close();
    }


    public abstract void handlerCallback(RPCClientHandler handler, boolean isActive);


    public RPCFuture sendRequestBySync(RPCRequest request) {
        RPCFuture rpcFuture = new RPCFuture(request);
        pending.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);
        return rpcFuture;
    }

    public RPCFuture sendRequestByAsync(RPCRequest request, AsyncRPCCallback callback) {
        RPCFuture rpcFuture = new RPCFuture(request, callback);
        pending.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);
        return rpcFuture;
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }


    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public String toString() {
        return "RPCClientHandler{" +"socketAddress=" + socketAddress + '}';
    }

}
