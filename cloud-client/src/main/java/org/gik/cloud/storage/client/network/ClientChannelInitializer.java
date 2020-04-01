package org.gik.cloud.storage.client.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.gik.cloud.storage.client.controller.MessageService;

public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private  MessageService messageService;

    public ClientChannelInitializer(MessageService messageService) {
        this.messageService = messageService;
    }

    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("clientHandler",new FileClientHandler(messageService));
    }
}
