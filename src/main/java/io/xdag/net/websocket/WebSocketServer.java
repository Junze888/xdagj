package io.xdag.net.websocket;

import io.netty.channel.Channel;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;


@Slf4j
@Getter
public class WebSocketServer {
    private  final String ClientHost;
    private final String ClientTag;
    private final int ServerPort;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel webSocketChannel;
    private final ExecutorService mainExecutor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder()
            .namingPattern("WebsocketServer-Main-Thread-%d")
            .daemon(true)
            .build());
    public WebSocketServer(String clientHost, String tag, int port) {
        this.ClientHost = clientHost;
        this.ClientTag = tag;
        this.ServerPort = port;
    }


    public void start() throws InterruptedException {
        log.info("Pool WebSocket enabled");
        mainExecutor.execute(() ->{
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup);
                b.channel(NioServerSocketChannel.class);
                b.childHandler(new WebsocketChannelInitializer(ClientHost,ClientTag,ServerPort));//initialize worker Group

                // 绑定端口并启动服务器
                webSocketChannel = b.bind("localhost", ServerPort).sync().channel();
                webSocketChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 关闭 EventLoopGroup
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }

    public void stop() {
        try {
            Objects.requireNonNull(webSocketChannel).closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Couldn't stop the Pool WebSocket server", e);
            Thread.currentThread().interrupt();
        }
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        mainExecutor.shutdown();
    }
}

