package org.gik.cloud.storage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.gik.cloud.storage.auth.AuthService;
import org.gik.cloud.storage.common.MessageType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileServerHandler extends ChannelInboundHandlerAdapter {


    public enum Stat {
        INIT, LENGTH, LENGTH_PASS, NAME, PASS
    }

    private final byte AUTH_CODE = 22;
    private final byte AUTH_CODE_ACCEPTED = 33;
    private final byte AUTH_CODE_FAIL = 44;
    private final byte GET_DIR = 55;
    private final byte SEND_FILE_FROM_SERVER = 66;


    private Stat curStat = Stat.INIT;
    private MessageType mType = MessageType.NONE;
    private String login = "";
    private String password = "";

    private ByteBuf buf;

    public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
        ByteBuf buf = ((ByteBuf) obj);

        while (buf.readableBytes() > 0) {
            getMessageType(buf);
            switch (mType) {
                case AUTH:
                    authentication(ctx, buf);
                    break;
                case GET_DIR:
                    sendDir(ctx);
                    break;
                case SEND_FILE_FROM_SERVER:
                    sendFile(ctx, buf);
                    break;
                case MOVE_FILE_FROM_SERVER:

                    break;
            }

        }


        if (buf.readableBytes() == 0) {
            curStat = Stat.INIT;
            buf.release();
        }
    }

    private void sendFile(ChannelHandlerContext ctx, ByteBuf buf) throws IOException {
        int length = getStringLength(buf);
        String name = getStringFromBuf(buf, length);

        sendByte(ctx, SEND_FILE_FROM_SERVER, false);
        Path path = Paths.get("serverStorage/" + login + "/" + name);
        sendInt(ctx, name.length(), false);
        sendString(ctx, name, true);
        sendLong(ctx, Files.size(path),true);
//
//        FileRegion region = new DefaultFileRegion
//                (new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
//        ctx.writeAndFlush(region).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendLong(ChannelHandlerContext ctx, long size, boolean flash) {
        buf = ByteBufAllocator.DEFAULT.buffer(8);
        buf.writeLong(size);
        if (flash) {
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(buf);
        }
    }

    private void sendDir(ChannelHandlerContext ctx) throws IOException {
        Files.list(Paths.get("serverStorage/" + login))
                .map(path -> path.getFileName().toString())
                .forEach(o -> {
                    sendByte(ctx, GET_DIR, false);
                    sendInt(ctx, o.length(), false);
                    sendString(ctx, o, true);
                });
    }

    private void sendInt(ChannelHandlerContext ctx, int strLeng, boolean flash) {
        buf = ByteBufAllocator.DEFAULT.buffer(4);
        buf.writeInt(strLeng);
        if (flash) {
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(buf);
        }
    }

    private void sendByte(ChannelHandlerContext ctx, byte code, boolean flash) {
        buf = ByteBufAllocator.DEFAULT.buffer(1);
        buf.writeByte(code);
        if (flash) {
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(buf);
        }

    }

    private void sendString(ChannelHandlerContext ctx, String o, boolean flash) {
        byte[] bytes = o.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.buffer(o.length());
        buf.writeBytes(bytes);
        if (flash) {
            ctx.writeAndFlush(buf);
        } else {
            ctx.write(bytes);
        }

    }

    private void authentication(ChannelHandlerContext ctx, ByteBuf buf) throws UnsupportedEncodingException {
        parsingAuth(buf);
        checkingUser(ctx, login, password);
    }

    private void parsingAuth(ByteBuf buf) throws UnsupportedEncodingException {
        int loginLength;
        int passLength;

        loginLength = getStringLength(buf, Stat.LENGTH, Stat.NAME);
        login = getStringFromBuf(buf, loginLength, Stat.NAME, Stat.LENGTH_PASS);
        passLength = getStringLength(buf, Stat.LENGTH_PASS, Stat.PASS);
        password = getStringFromBuf(buf, passLength, Stat.PASS, Stat.INIT);
    }

    private String getStringFromBuf(ByteBuf buf, int strLength) throws UnsupportedEncodingException {
        Stat statIn = Stat.NAME;
        Stat statOut = Stat.INIT;
        return getStringFromBuf(buf, strLength, statIn, statOut);
    }

    private String getStringFromBuf(ByteBuf buf, int strLength, Stat statIn, Stat statOut) throws UnsupportedEncodingException {
        String stringOut = "";
        if (curStat == statIn) {
            byte[] name = new byte[strLength];
            buf.readBytes(name);
            curStat = statOut;
            stringOut = new String(name, StandardCharsets.UTF_8);
        }
        return stringOut;
    }

    private int getStringLength(ByteBuf buf) {
        Stat statIn = Stat.LENGTH;
        Stat statOut = Stat.NAME;
        return getStringLength(buf, statIn, statOut);
    }

    private int getStringLength(ByteBuf buf, Stat statIn, Stat statOut) {
        int stringLength = 0;
        if (curStat == statIn) {
            if (buf.readableBytes() >= 4) {
                stringLength = buf.readInt();
                curStat = statOut;
            }
        }
        return stringLength;
    }

    private void checkingUser(ChannelHandlerContext ctx, String login, String password) {
        if (new AuthService().isLegitUser(login, password)) {
            sendByte(ctx, AUTH_CODE_ACCEPTED, true);

        } else {
            sendByte(ctx, AUTH_CODE_FAIL, true);
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
            } else if (messageType == SEND_FILE_FROM_SERVER) {
                mType = MessageType.SEND_FILE_FROM_SERVER;
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush("ERR: " + cause.getClass().getSimpleName() + " : " +
                    cause.getMessage() + '\n').addListener(ChannelFutureListener.CLOSE);
        }
    }


}
