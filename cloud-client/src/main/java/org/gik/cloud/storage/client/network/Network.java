package org.gik.cloud.storage.client.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.gik.cloud.storage.client.controller.MessageService;

import java.io.Closeable;

public class Network implements Closeable {
    private MessageService messageService;
    private Channel currentChannel;

    public Network(MessageService messageService) {
        this.messageService = messageService;
    }

    public Channel getChannel() {
        return currentChannel;
    }

    private final String HOST = System.getProperty("host", "127.0.0.1");
    private final int PORT = Integer.parseInt(System.getProperty("port", "8021"));
    private EventLoopGroup group = new NioEventLoopGroup();

    public void run() throws Exception {

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ClientChannelInitializer(this.messageService));
        currentChannel = b.connect(HOST, PORT).sync().channel();

    }


    @Override
    public void close() {
        currentChannel.close();
        group.shutdownGracefully();
    }

}
