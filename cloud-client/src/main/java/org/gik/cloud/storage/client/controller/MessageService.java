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

    public void sendMessage(MessageType messageType, String string, String pass) throws Exception {
        if (network.getChannel() == null) {
            network.run();
            channel = network.getChannel();
        }
        switch (messageType) {
            case AUTH:
                sendAuth(string, pass);
                break;
            case GET_DIR:
                getDirFromServer(string);
                break;
            case SEND_FILE_FROM_SERVER:
                getFileFromServer(string);
                break;
        }
    }

    private void getFileFromServer(String fileName) {
        sendByte(SEND_FILE_FROM_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, true);
    }

    private void sendString(String s, boolean flash) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.buffer(s.length());
        buf.writeBytes(bytes);
        if (flash) {
            channel.writeAndFlush(buf);
        } else {
            channel.write(buf);
        }
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
        sendByte(GET_DIR, true);
    }

    private void sendAuth(String login, String pass) {
        sendByte(AUTH_CODE,false);
        sendInt(login.length());
        sendString(login,false);
        sendInt(pass.length());
        sendString(pass,true);
//        ByteBuf buf;
//        buf = ByteBufAllocator.DEFAULT.buffer(1);
//        buf.writeByte(AUTH_CODE);
//        channel.writeAndFlush(buf);
//
//        buf = ByteBufAllocator.DEFAULT.buffer(4);
//        buf.writeInt(login.length());
//        channel.writeAndFlush(buf);
//
//        byte[] bytesLogin = login.getBytes(StandardCharsets.UTF_8);
//        buf = ByteBufAllocator.DEFAULT.buffer(login.length());
//        buf.writeBytes(bytesLogin);
//        channel.writeAndFlush(buf);
//
//        buf = ByteBufAllocator.DEFAULT.buffer(4);
//        buf.writeInt(pass.length());
//        channel.writeAndFlush(buf);
//
//        byte[] bytesPass = pass.getBytes(StandardCharsets.UTF_8);
//        buf = ByteBufAllocator.DEFAULT.buffer(pass.length());
//        buf.writeBytes(bytesPass);
//        channel.writeAndFlush(buf);
    }


}
