package org.gik.cloud.storage;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class FileServerInitializer extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel sc) {
        ChannelPipeline pipeline = sc.pipeline();
        pipeline.addLast("fileServerHandler",new FileServerHandler());
    }
}
