package com.eagle.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by HXJ on 2015/9/30.
 */
public class TimeClient {
    private Selector selector;
    public static void main(String[] args) throws IOException {
        new TimeClient().start();
    }

    public void start() throws IOException {
        SocketChannel sc = SocketChannel.open();
        selector = Selector.open();
        sc.configureBlocking(false);
        boolean connected = sc.connect(new InetSocketAddress("127.0.0.1", 6000));
        sc.register(selector, SelectionKey.OP_CONNECT);
        if (connected) {
            sc.register(selector, SelectionKey.OP_READ);
            doWrite(sc);
        }
        while (true) {
            selector.select(1000);
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()){
                SelectionKey key = keys.next();
                keys.remove();
                if (!key.isValid()) continue;
                if (key.isConnectable()) {
                    sc.finishConnect();
                    sc.register(selector, SelectionKey.OP_READ);
                    doWrite(sc);
                }
                if (key.isReadable()) read(key);
            }
        }
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
            System.out.println("==========>>> Now is : " + body);
            //doWrite(sc);
        }
    }

    protected void doWrite(SocketChannel sc) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate("QUERY TIME ORDER".getBytes().length);
        buffer.put("QUERY TIME ORDER".getBytes());
        buffer.flip();
        sc.write(buffer);
    }
}
