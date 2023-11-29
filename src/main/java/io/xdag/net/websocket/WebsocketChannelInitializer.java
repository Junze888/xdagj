package io.xdag.net.websocket;

import io.netty.channel.ChannelInitializer;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
@Slf4j
public class WebsocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    private  final String ClientHost;
    private final String ClientTag;
    private final int ServerPort;
    public WebsocketChannelInitializer(String clientHost, String ClientTag, int ServerPort){
        this.ClientHost = clientHost;
        this.ClientTag = ClientTag;
        this.ServerPort = ServerPort;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        try {
            InetSocketAddress address =  ch.remoteAddress();
            log.debug("New {} channel: remoteAddress = {}:{}",  "inbound" ,
                    address.getAddress().getHostAddress(), address.getPort());

            ch.pipeline().addLast("http-codec",new HttpServerCodec());//设置解码器
            ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));//聚合器，使用websocket会用到
            ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());//用于大数据的分区传输
            ch.pipeline().addLast("handler",new PoolHandShakeHandler(ClientHost,ClientTag,ServerPort));
        } catch (Exception e) {
            log.error("Unexpected error: [{}]", e.getMessage(), e);
        }
    }
}
