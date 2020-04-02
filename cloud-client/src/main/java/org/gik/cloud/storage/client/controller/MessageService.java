package org.gik.cloud.storage.client.controller;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import javafx.application.Platform;
import org.gik.cloud.storage.client.network.Network;
import org.gik.cloud.storage.common.MessageType;

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

    private final byte AUTH_CODE = 22;
    private final byte GET_DIR = 55;
    private final byte SEND_FILE_FROM_SERVER = 66;
    private final byte DELETE_FILE_ON_SERVER = 77;
    private final byte SEND_FILE_TO_SERVER = 88;

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
            case DELETE:
                deleteFileOnServer(string);
                break;
            case SEND_FILE_TO_SERVER:
                sendFileToServer(string);
                break;
            default:
                break;
        }
    }

    private void sendFileToServer(String fileName) throws IOException {
        sendByte(SEND_FILE_TO_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, false);
        Path path = Paths.get(controller.getUserDir() + fileName);
        sendLong(Files.size(path), false);
        FileRegion region = new DefaultFileRegion
                (new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        channel.writeAndFlush(region).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private void sendLong(long size, boolean flash) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8);
        buf.writeLong(size);
        if (flash) {
            channel.writeAndFlush(buf);
        } else {
            channel.write(buf);
        }
    }


    private void deleteFileOnServer(String fileName) {
        sendByte(DELETE_FILE_ON_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, true);

        Platform.runLater(() -> {
            try {
                controller.reloadUIServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void getFileFromServer(String fileName) {
        sendByte(SEND_FILE_FROM_SERVER, false);
        sendInt(fileName.length());
        sendString(fileName, true);
    }

    private void getDirFromServer(String userDir) {
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

}
