package org.gik.cloud.storage.client.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import org.gik.cloud.storage.client.network.Network;
import org.gik.cloud.storage.common.MessageType;

import java.nio.charset.StandardCharsets;

public class MessageService {

    private Network network;
    private Channel channel;
    private Controller controller;

    ByteBuf buf;

    private final byte AUTH_CODE =  22;
    private final byte GET_DIR =  55;
    private final byte SEND_FILE_FROM_SERVER = 66;

    public Controller getController() {
        return controller;
    }

    public MessageService(Controller controller) {
        this.network = new Network(this);
        this.controller = controller;
    }

    public void sendMessage(MessageType messageType, String parameter) throws Exception {
        sendMessage(messageType, parameter, "");
    }

    public void sendMessage(MessageType messageType, String parameter, String pass) throws Exception {
        if (network.getChannel() == null) {
            network.run();
            channel = network.getChannel();
        }
        switch (messageType) {
            case AUTH:
                sendAuth(parameter, pass);
                break;
            case GET_DIR:
                getDirFromServer(parameter);
                break;
            case SEND_FILE_FROM_SERVER:
                getFileFromServer(parameter);
                break;
        }
    }

    private void getFileFromServer(String filesName) {
//        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(1);
//        buf.writeByte((byte)66);
//        channel.writeAndFlush(buf);
        sendByte(SEND_FILE_FROM_SERVER, false);
        sendInt(filesName.length());
        sendString(filesName);
    }

    private void sendString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.buffer(s.length());
        buf.writeBytes(bytes);
        channel.writeAndFlush(buf);
    }

    private void sendInt(int length) {
        buf = ByteBufAllocator.DEFAULT.buffer(4);
        buf.writeInt(length);
        channel.write(buf);
    }

    private void sendByte(byte byteCode, boolean flash) {
        buf = ByteBufAllocator.DEFAULT.buffer(1);
        buf.writeByte(byteCode);
        if (flash) {
            channel.writeAndFlush(buf);
        } else {
            channel.write(buf);
        }
    }

    private void getDirFromServer(String userDir) {
        ByteBuf buf;
        buf = ByteBufAllocator.DEFAULT.buffer(1);
        buf.writeByte(GET_DIR);
        channel.writeAndFlush(buf);

    }

    private void sendAuth(String login, String pass) {
        ByteBuf buf;
        buf = ByteBufAllocator.DEFAULT.buffer(1);
        buf.writeByte(AUTH_CODE);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.buffer(4);
        buf.writeInt(login.length());
        channel.writeAndFlush(buf);

        byte[] bytesLogin = login.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.buffer(login.length());
        buf.writeBytes(bytesLogin);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.buffer(4);
        buf.writeInt(pass.length());
        channel.writeAndFlush(buf);

        byte[] bytesPass = pass.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.buffer(pass.length());
        buf.writeBytes(bytesPass);
        channel.writeAndFlush(buf);
    }


}
