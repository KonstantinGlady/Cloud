package org.gik.cloud.storage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.gik.cloud.storage.auth.AuthService;
import org.gik.cloud.storage.common.MessageType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.gik.cloud.storage.common.MessageCode.*;

public class FileServerHandler extends ChannelInboundHandlerAdapter {

    public static final String SERVER_STORAGE = "serverStorage/";

    private enum Stat {
        INIT, LENGTH, FILE_LENGTH, WRITE_FILE, NAME, PASS;
    }

    private Stat curStat = Stat.INIT;
    private MessageType mType = MessageType.NONE;
    private String login = "";
    private String password = "";

    private BufferedOutputStream out;
    private ByteBuf buf;
    private ChannelHandlerContext ctx;

    private long fileLengthLong;
    private long fileLengthLongReceived;

    private String fileName;
    private int nameLength;

    public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
        ByteBuf buf = ((ByteBuf) obj);
        this.ctx = ctx;


        while (buf.readableBytes() > 0) {
            getMessageType(buf);
            switch (mType) {
                case AUTH:
                    authentication(buf);
                    break;
                case GET_DIR:
                    sendDir();
                    break;
                case COPY_FILE_FROM_SERVER:
                    sendFile(buf, false);
                    break;
                case MOVE_FILE_FROM_SERVER:
                    sendFile(buf, true);
                    break;
                case COPY_FILE_TO_SERVER:
                    getFile(buf, false);
                    break;
                case MOVE_FILE_TO_SERVER:
                    getFile(buf, true);
                    break;
                case DELETE:
                    deleteFile(buf);
                    break;
            }
        }

        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    private void getFile(ByteBuf buf, boolean delOrigFile) throws IOException {
        getNameLength(buf);
        getFileName(buf, nameLength);
        getStringLengthLong(buf);
        if (curStat == Stat.WRITE_FILE) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                fileLengthLongReceived++;
                if (fileLengthLongReceived >= fileLengthLong) {
                    out.close();
                    curStat = Stat.INIT;
                    mType = MessageType.NONE;
                    fileLengthLongReceived = 0;
                    if (delOrigFile) {
                        deleteFileOnClient(nameLength, fileName);
                    }
                    nameLength = 0;
                    fileName = "";
                    sendByte(REFRESH_UI, true);
                    break;
                }
            }
        }

    }

    private void deleteFileOnClient(int length, String name) {
        sendByte(DELETE_FILE_ON_CLIENT, false);
        sendInt(length);
        sendString(name, true);
    }

    private void getStringLengthLong(ByteBuf buf) {
        if (curStat == Stat.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLengthLong = buf.readLong();
                curStat = Stat.WRITE_FILE;
            }
        }
    }

    private void getNameLength(ByteBuf buf) {
        if (curStat == Stat.LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                curStat = Stat.NAME;
            }
        }
    }

    private void getFileName(ByteBuf buf, int strLength) throws FileNotFoundException {
        if (curStat == Stat.NAME) {
            byte[] bytes = new byte[strLength];

            int i = 0;
            while (i < strLength) {
                bytes[i] = buf.readByte();
                i++;
            }
            fileName = new String(bytes, StandardCharsets.UTF_8);
            out = new BufferedOutputStream(new FileOutputStream(SERVER_STORAGE + login + "/" + fileName));
            curStat = Stat.FILE_LENGTH;
        }
    }


    private void deleteFile(ByteBuf buf) throws IOException {
        int length = getStringLength(buf);
        String name = getStringFromBuf(buf, length);

        Path path = Paths.get(SERVER_STORAGE + login + "/" + name);
        Files.delete(path);
        curStat = Stat.INIT;
        sendByte(REFRESH_UI, true);
    }

    private void sendFile(ByteBuf buf, boolean delOrigFile) throws IOException {
        if (delOrigFile) {
            sendByte(MOVE_FILE_FROM_SERVER, false);
        } else {
            sendByte(COPY_FILE_FROM_SERVER, false);
        }
        int length = getStringLength(buf);
        String name = getStringFromBuf(buf, length);
        Path path = Paths.get(SERVER_STORAGE + login + "/" + name);
        sendInt(name.length());
        sendString(name, false);
        sendLong(Files.size(path));

        FileRegion region = new DefaultFileRegion
                (new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        ctx.writeAndFlush(region).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        sendByte(REFRESH_UI, true);
    }

    private void sendLong(long size) {
        buf = ByteBufAllocator.DEFAULT.buffer(8);
        buf.writeLong(size);
        ctx.write(buf);
    }

    private void sendDir() throws IOException {
        sendByte(CLEAR_SERVER_LIST, true);
        Files.list(Paths.get(SERVER_STORAGE + login))
                .map(path -> path.getFileName().toString())
                .forEach(o -> {
                    sendByte(GET_DIR, false);
                    sendInt(o.length());
                    sendString(o, true);
                });
        curStat = Stat.INIT;
    }

    private void sendInt(int length) {
        buf = ByteBufAllocator.DEFAULT.buffer(4);
        buf.writeInt(length);
        ctx.write(buf);
    }


    private void sendByte(byte code, boolean flash) {
        buf = ByteBufAllocator.DEFAULT.buffer(1);
        buf.writeByte(code);
        if (flash) {
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(buf);
        }

    }

    private void sendString(String o, boolean flash) {
        byte[] bytes = o.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.buffer(o.length());
        buf.writeBytes(bytes);
        if (flash) {
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(buf);
        }

    }

    private void authentication(ByteBuf buf) {
        parsingAuth(buf);
        checkingUser(login, password);
    }

    private void parsingAuth(ByteBuf buf) {
        int loginLength;
        int passLength;

        loginLength = getStringLength(buf);
        login = getStringFromBuf(buf, loginLength);
        curStat = Stat.LENGTH;

        passLength = getStringLength(buf);
        password = getStringFromBuf(buf, passLength);
        curStat = Stat.INIT;
    }

    private String getStringFromBuf(ByteBuf buf, int strLength) {
        String stringOut = "";
        if (curStat == Stat.NAME) {
            byte[] name = new byte[strLength];
            buf.readBytes(name);
            curStat = Stat.INIT;
            stringOut = new String(name, StandardCharsets.UTF_8);
        }
        return stringOut;

    }

    private int getStringLength(ByteBuf buf) {
        int stringLength = 0;
        if (curStat == Stat.LENGTH) {
            if (buf.readableBytes() >= 4) {
                stringLength = buf.readInt();
                curStat = Stat.NAME;
            }
        }
        return stringLength;
    }

    private void checkingUser(String login, String password) {
        if (new AuthService().isLegitUser(login, password)) {
            sendByte(AUTH_CODE_ACCEPTED, true);

        } else {
            sendByte(AUTH_CODE_FAIL, true);
        }
    }

    private void getMessageType(ByteBuf buf) {
        if (curStat == Stat.INIT) {
            byte messageType = buf.readByte();
            curStat = Stat.LENGTH;
            if (messageType == AUTH_CODE) {
                mType = MessageType.AUTH;
            } else if (messageType == GET_DIR) {
                mType = MessageType.GET_DIR;
            } else if (messageType == COPY_FILE_FROM_SERVER) {
                mType = MessageType.COPY_FILE_FROM_SERVER;
            } else if (messageType == DELETE_FILE_ON_SERVER) {
                mType = MessageType.DELETE;
            } else if (messageType == COPY_FILE_TO_SERVER) {
                mType = MessageType.COPY_FILE_TO_SERVER;
            } else if (messageType == MOVE_FILE_TO_SERVER) {
                mType = MessageType.MOVE_FILE_TO_SERVER;
            } else if (messageType == MOVE_FILE_FROM_SERVER) {
                mType = MessageType.MOVE_FILE_FROM_SERVER;
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush("ERR: " + cause.getClass().getSimpleName() + " : " +
                    cause.getMessage() + '\n').addListener(ChannelFutureListener.CLOSE);
        }
    }


}
