package org.gik.cloud.storage.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import javafx.application.Platform;
import org.gik.cloud.storage.client.controller.Controller;
import org.gik.cloud.storage.client.controller.MessageService;
import org.gik.cloud.storage.common.MessageType;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileClientHandler extends ChannelInboundHandlerAdapter {

    public enum Stat {
        INIT, LENGTH, FILE_LENGTH, NAME, WRITE_FILE;
    }
    private MessageType mType = MessageType.NONE;
    private Stat curState = Stat.INIT;
    private BufferedOutputStream out;
    private long fileLengthLong;
    private long fileLengthLongReceived;

    private static final byte AUTH_CODE = 22;
    private static final byte AUTH_CODE_ACCEPTED = 33;
    private static final byte AUTH_CODE_FAIL = 44;
    private static final byte GET_DIR_CODE = 55;
    private static final byte GET_FILE = 66;
    private final byte REFRESH_UI = 99;

   // private final MessageService mService;
    private Controller controller;

    public FileClientHandler(MessageService messageService) {
    //    this.mService = messageService;
        this.controller = messageService.getController();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        while (buf.readableBytes() > 0) {
            getMessageType(buf);
            switch (mType) {
                case AUTH:
                    authCodAccepted();
                    break;
                case AUTH_CODE_FAIL:
                    warningWindow();
                    break;
                case GET_DIR:
                    getDirFromServer(buf);
                    break;
                case SEND_FILE_FROM_SERVER:
                    getFileFromServer(buf);
                    break;
                case REFRESH_UI:
                    refreshUI();
                    break;
                case NONE:
                    break;
            }

        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    private void refreshUI() {
        Platform.runLater(() -> {
            try {
                controller.reloadUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        mType = MessageType.NONE;
    }

    private void warningWindow() {
        Platform.runLater(Controller::warningWindow);
    }

    private void getFileFromServer(ByteBuf buf) throws Exception {
        int nameLength = getStringLength(buf);
        getStringFromBuf(buf, nameLength, true);
        getStringLengthLong(buf);
        if (curState == Stat.WRITE_FILE) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                fileLengthLongReceived++;
                if (fileLengthLongReceived >= fileLengthLong) {
                    out.close();
                    curState = Stat.INIT;
                    mType = MessageType.NONE;
                    fileLengthLongReceived = 0;
                    Platform.runLater(() -> {
                        try {
                            controller.reloadUILocal();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                }
            }
        }
    }

    private void getStringLengthLong(ByteBuf buf) {
        if (curState == Stat.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLengthLong = buf.readLong();
                curState = Stat.WRITE_FILE;
            }
        }
    }

    private void getDirFromServer(ByteBuf buf) throws FileNotFoundException {
        int strLength = getStringLength(buf);
        String strFromBuf = getStringFromBuf(buf, strLength, false);
        Platform.runLater(() -> controller.fileListServer.getItems().add(strFromBuf));
        curState = Stat.INIT;
    }

    private String getStringFromBuf(ByteBuf buf, int strLength, boolean nextStateGetFile) throws FileNotFoundException {
        String str = "";
        if (curState == Stat.NAME) {
            byte[] bytes = new byte[strLength];

            int i = 0;
            while (i < strLength) {
                bytes[i] = buf.readByte();
                i++;
            }
            // buf.readBytes(bytes);
            str = new String(bytes, StandardCharsets.UTF_8);
            if (nextStateGetFile) {
                out = new BufferedOutputStream(new FileOutputStream(controller.getUserDir() + str));
                curState = Stat.FILE_LENGTH;
            } else {
                curState = Stat.LENGTH;
            }
        }
        return str;
    }

    private int getStringLength(ByteBuf buf) {
        int strLength = 0;
        if (curState == Stat.LENGTH) {
            if (buf.readableBytes() >= 4) {
                strLength = buf.readInt();
                curState = Stat.NAME;
            }
        }
        return strLength;
    }

    private void getMessageType(ByteBuf buf) {
        if (curState == Stat.INIT) {
            byte messageType = buf.readByte();
            switch (messageType) {
                case AUTH_CODE_ACCEPTED:
                    mType = MessageType.AUTH;
                    curState = Stat.LENGTH;
                    break;
                case AUTH_CODE_FAIL:
                    mType = MessageType.AUTH_CODE_FAIL;
                    curState = Stat.INIT;
                    break;
                case GET_DIR_CODE:
                    mType = MessageType.GET_DIR;
                    curState = Stat.LENGTH;
                    break;
                case GET_FILE:
                    mType = MessageType.SEND_FILE_FROM_SERVER;
                    curState = Stat.LENGTH;
                    break;
                case REFRESH_UI:
                    mType = MessageType.REFRESH_UI;
                    break;
                default:
                    mType = MessageType.NONE;
                    curState = Stat.INIT;
                    break;

            }
        }
    }

    private void authCodAccepted() throws Exception {
        controller.authPanel.setVisible(false);
        controller.cloudPanel.setVisible(true);
        curState = Stat.INIT;
        mType = MessageType.NONE;
        controller.reloadUI();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
