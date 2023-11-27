package io.xdag.websocket;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Nullable;

@Slf4j
public class WebSocketServer {
    private  final String ClientHost;
    private final String ClientTag;
    private final int ServerPort;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private @Nullable ChannelFuture webSocketChannel;

    public WebSocketServer(String clientHost, String tag, int port) {
        this.ClientHost = clientHost;
        this.ClientTag = tag;
        this.ServerPort = port;
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
    }


    public void start() throws InterruptedException {
        log.info("Pool WebSocket enabled");
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast("logging",new LoggingHandler("INFO"));//set log listener, level debug
                        ch.pipeline().addLast("http-codec",new HttpServerCodec());//http decoder
                        ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));//http send segmented data, need to aggregate
                        ch.pipeline().addLast("handler", new PoolHandShakeHandler(ClientHost, ClientTag, ServerPort));//pool handler write by ourselves
                    }
                });//initialize worker Group
        webSocketChannel  = b.bind("localhost",ServerPort);
        try {
            webSocketChannel.sync();
        } catch (InterruptedException e) {
            log.error("The Pool WebSocket server couldn't be started", e);
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        try {
            Objects.requireNonNull(webSocketChannel).channel().close().sync();
        } catch (InterruptedException e) {
            log.error("Couldn't stop the Pool WebSocket server", e);
            Thread.currentThread().interrupt();
        }
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }
}

