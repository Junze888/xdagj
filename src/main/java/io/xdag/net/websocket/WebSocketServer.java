package io.xdag.net.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
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

import io.xdag.utils.NettyUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
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
            try {
                // 绑定端口并启动服务器
                webSocketChannel = b.bind("localhost", ServerPort).channel();
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

