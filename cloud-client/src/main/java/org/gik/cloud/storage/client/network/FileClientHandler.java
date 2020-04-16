package org.gik.cloud.storage.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import javafx.application.Platform;
import org.gik.cloud.storage.client.controller.Controller;
import org.gik.cloud.storage.client.controller.MessageService;
import org.gik.cloud.storage.common.MessageType;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.gik.cloud.storage.common.MessageCode.*;

public class FileClientHandler extends ChannelInboundHandlerAdapter  {


    private enum Stat {
        INIT, LENGTH, FILE_LENGTH, NAME, WRITE_FILE;
    }

    private MessageType mType = MessageType.NONE;
    private Stat curState = Stat.INIT;
    private BufferedOutputStream out;
    private long fileLengthLong;
    private long fileLengthLongReceived;
    private String deleteThisFileName = "";
    private Controller controller;
    private MessageService mService;

    public FileClientHandler(MessageService messageService) {
        this.mService = messageService;
        controller = Controller.getInstance();
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
                case COPY_FILE_FROM_SERVER:
                    getFileFromServer(buf, false);
                    break;
                case MOVE_FILE_FROM_SERVER:
                    getFileFromServer(buf, true);
                    break;
                case REFRESH_UI:
                    refreshUI();
                    break;
                case DELETE_FILE_ON_CLIENT:
                    deleteFile(buf);
                    break;
                case CLEAR_SERVER_LIST:
                    clearServerList();
                    break;
                case NONE:
                    break;
            }

        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    private void clearServerList() {
        controller.clearServerList();
    }

    private void deleteFile(ByteBuf buf) throws IOException {
        int length = getStringLength(buf);
        String name = getStringFromBuf(buf, length, false);
        Platform.runLater(() -> {
            try {
                controller.deleteFile(name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mType = MessageType.NONE;
        curState = Stat.INIT;
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

    private void getFileFromServer(ByteBuf buf, boolean delOrigFile) throws Exception {
        int length = getStringLength(buf);
        deleteThisFileName = ((deleteThisFileName.equals("")) ? getStringFromBuf(buf, length, true) : deleteThisFileName);
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
                    if (delOrigFile) {
                        mService.deleteFileOnServer(deleteThisFileName);
                    }
                    deleteThisFileName = "";
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

    private void getDirFromServer(ByteBuf buf) throws Exception {
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
                case GET_DIR:
                    mType = MessageType.GET_DIR;
                    curState = Stat.LENGTH;
                    break;
                case COPY_FILE_FROM_SERVER:
                    mType = MessageType.COPY_FILE_FROM_SERVER;
                    curState = Stat.LENGTH;
                    break;
                case MOVE_FILE_FROM_SERVER:
                    mType = MessageType.MOVE_FILE_FROM_SERVER;
                    curState = Stat.LENGTH;
                    break;
                case REFRESH_UI:
                    mType = MessageType.REFRESH_UI;
                    break;
                case DELETE_FILE_ON_CLIENT:
                    mType = MessageType.DELETE_FILE_ON_CLIENT;
                    curState = Stat.LENGTH;
                    break;
                case CLEAR_SERVER_LIST:
                    mType = MessageType.CLEAR_SERVER_LIST;
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
