package org.gik.cloud.storage.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.gik.cloud.storage.client.controller.Controller;
import org.gik.cloud.storage.client.controller.MessageService;
import org.gik.cloud.storage.common.MessageType;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileClientHandler extends ChannelInboundHandlerAdapter {


    private MessageType mType = MessageType.NONE;
    private State curState = State.INIT;
    private BufferedOutputStream out;
    private long fileLengthLong;
    private long fileLengthLongReceived;

    private static final byte AUTH_CODE = 22;
    private static final byte AUTH_CODE_ACCEPTED = 33;
    private static final byte AUTH_CODE_FAIL = 44;
    private static final byte GET_DIR_CODE = 55;
    private static final byte GET_FILE = 66;

    private final MessageService mService;
    private Controller controller;

    public FileClientHandler(MessageService messageService) {
        this.mService = messageService;
        this.controller = messageService.getController();
    }

    public enum State {
        INIT, LENGTH, FILE_LENGTH, NAME, WRITE_FILE;
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
                case GET_DIR:
                    getDirFromServer(buf);
                    break;
                case SEND_FILE_FROM_SERVER:
                    getFileFromServer(buf);
                    break;
            }

        }
        if (buf.readableBytes() == 0) {
            if (mType != MessageType.SEND_FILE_FROM_SERVER) {
                mType = MessageType.NONE;
                curState = State.INIT;
            }
            buf.release();
        }
    }

    private void getFileFromServer(ByteBuf buf) throws Exception {
        int nameLength = getStringLength(buf);
        String name = getStringFromBuf(buf, nameLength, true);
        getStringLengthLong(buf);
        if (curState == State.WRITE_FILE) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                fileLengthLongReceived++;
                if (fileLengthLongReceived >= fileLengthLong) {
                    out.close();
                    System.out.println("close client");
                    curState = State.INIT;
                    mType = MessageType.NONE;
                   // controller.reloadUILocal();
                    break;
                }
            }
        }
    }

    private void getStringLengthLong(ByteBuf buf) {
        if (curState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLengthLong = buf.readLong();
                curState = State.WRITE_FILE;
            }
        }
    }

    private void getDirFromServer(ByteBuf buf) throws FileNotFoundException {
        int strLength = getStringLength(buf);
        String strFromBuf = getStringFromBuf(buf, strLength, false);
        mService.getController().filesListServer.getItems().add(strFromBuf);
    }

    private String getStringFromBuf(ByteBuf buf, int strLength, boolean nextStateGetFile) throws FileNotFoundException {
        String str = "";
        if (curState == State.NAME) {
            byte[] bytes = new byte[strLength];

            int i = 0;
            while (i < strLength) {
                bytes[i] = buf.readByte();
                i++;
            }
            // buf.readBytes(bytes);
            str = new String(bytes, StandardCharsets.UTF_8);
            if (nextStateGetFile) {
                out = new BufferedOutputStream(new FileOutputStream("localStorage/" + controller.getUserDir() + "/" + str));
                curState = State.FILE_LENGTH;
            } else {
                curState = State.LENGTH;
            }
        }
        return str;
    }

    private int getStringLength(ByteBuf buf) {
        int strLength = 0;
        if (curState == State.LENGTH) {
            if (buf.readableBytes() >= 4) {
                strLength = buf.readInt();
                curState = State.NAME;
            }
        }
        return strLength;
    }

    private void getMessageType(ByteBuf buf) {
        if (curState == State.INIT) {
            byte messageType = buf.readByte();
            switch (messageType) {
                case AUTH_CODE_ACCEPTED:
                    mType = MessageType.AUTH;
                    curState = State.LENGTH;
                    break;
                case AUTH_CODE_FAIL:
                    System.out.println("try again");//заменить окошком
                    break;
                case GET_DIR_CODE:
                    mType = MessageType.GET_DIR;
                    curState = State.LENGTH;
                    break;
                case GET_FILE:
                    mType = MessageType.SEND_FILE_FROM_SERVER;
                    curState = State.LENGTH;
            }
        }
    }

    private void authCodAccepted() throws Exception {
        mService.getController().authPanel.setVisible(false);
        mService.getController().cloudPanel.setVisible(true);
        controller.reloadUI();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
