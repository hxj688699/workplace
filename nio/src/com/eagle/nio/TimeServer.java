package com.eagle.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by HXJ on 2015/9/30.
 */
public class TimeServer {
    private Selector selector;
    public static void main(String[] args) throws IOException {
        new TimeServer().start();
    }

    public void start() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        selector = Selector.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(6000));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select(1000);
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()){
                SelectionKey key = keys.next();
                keys.remove();
                if (!key.isValid()) continue;
                if (key.isAcceptable()) accept(key);
                if (key.isReadable()) read(key);
            }
        }
    }

    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        System.out.println("=============>>> A new connect");
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
    }

    protected void read(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(64);
        int readBytes = sc.read(buffer);
        if (readBytes > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, "UTF-8");
            System.out.println("The time server recevice order : " + body);
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) : "BAD ORDER";
            doWrite(sc, currentTime);
        }
    }

    protected void doWrite(SocketChannel sc, String resp) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(resp.getBytes().length);
        buffer.put(resp.getBytes());
        buffer.flip();
        sc.write(buffer);
    }
}
