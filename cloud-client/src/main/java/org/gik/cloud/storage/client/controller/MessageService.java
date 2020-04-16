package org.gik.cloud.storage.client.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.gik.cloud.storage.client.network.Network;
import org.gik.cloud.storage.common.MessageType;
import static org.gik.cloud.storage.common.MessageCode.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MessageService {

    private Network network;
    private Channel channel;
    private Controller controller;
    ByteBuf buf;


    public MessageService( ) {
        this.network = new Network(this);
        this.controller = Controller.getInstance();
    }

    public void sendMessage(MessageType messageType, String... str) throws Exception {
        if (network.getChannel() == null) {
            network.run();
            channel = network.getChannel();
        }
        switch (messageType) {
            case AUTH:
                sendAuth(str[0], str[1]);
                break;
            case GET_DIR:
                getDirFromServer();
                break;
            case COPY_FILE_FROM_SERVER:
                getFileFromServer(str[0]);
                break;
            case MOVE_FILE_FROM_SERVER:
                getFileFromServerAndDelete(str[0]);
                break;
            case DELETE:
                deleteFileOnServer(str[0]);
                break;
            case COPY_FILE_TO_SERVER:
                sendFile(str[0], false);
                break;
            case MOVE_FILE_TO_SERVER:
                sendFile(str[0], true);
                break;
            default:
                break;
        }
    }

    private void sendFile(String fileName, boolean deleteFile) throws IOException {
        if (deleteFile) {
            sendByte(MOVE_FILE_TO_SERVER, false);
        } else {
            sendByte(COPY_FILE_TO_SERVER, false);
        }
        sendInt(fileName.length());
        sendString(fileName, false);
        Path path = Paths.get(controller.getUserDir() + fileName);
        sendLong(Files.size(path));
        FileRegion region = new DefaultFileRegion
                (new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        channel.writeAndFlush(region).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void sendLong(long size) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8);
        buf.writeLong(size);
        channel.write(buf);
    }

    public void deleteFileOnServer(String fileName) {
        sendByte(DELETE_FILE_ON_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, true);
    }

    private void getFileFromServer(String fileName) {
        sendByte(COPY_FILE_FROM_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, true);
    }
    private void getFileFromServerAndDelete(String fileName) {
        sendByte(MOVE_FILE_FROM_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, true);
    }

    private void getDirFromServer() {
        sendByte(GET_DIR, true);
    }

    private void sendAuth(String login, String pass) {
        sendByte(AUTH_CODE, false);
        sendInt(login.length());
        sendString(login, false);
        sendInt(pass.length());
        sendString(pass, true);
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

    public void close() {
        network.close();
    }
}
