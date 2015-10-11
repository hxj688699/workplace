package com.eagle.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * Created by HXJ on 2015/10/11.
 */
public class AioTimeServer {
    public static void main(String[] args) {
        new Thread(new AsyncServerHandler(6666), "AIO-Server").start();
    }
}

class AsyncServerHandler implements Runnable {
    private CountDownLatch latch;
    private int port;
    private AsynchronousServerSocketChannel asynchronousServerSocketChannel;
    public AsyncServerHandler(int port) {
        this.port = port;
        try {
            asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
            asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        latch = new CountDownLatch(1);
        doAccept();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void doAccept() {
        asynchronousServerSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, AsyncServerHandler>() {
            @Override
            public void completed(final AsynchronousSocketChannel socketChannel, AsyncServerHandler attachment) {
                attachment.asynchronousServerSocketChannel.accept(attachment, this);
                ByteBuffer buffer = ByteBuffer.allocate(128);
                socketChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        attachment.flip();
                        byte[] body = new byte[attachment.remaining()];
                        attachment.get(body);
                        try {
                            String req = new String(body, "UTF-8");
                            System.out.println("The time server recevice order : " + req);
                            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req) ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) : "BAD ORDER";
                            doWrite(currentTime);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        try {
                            socketChannel.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    void doWrite(String time) {
                        byte[] bytes = time.getBytes();
                        ByteBuffer writeBuffer = ByteBuffer.allocate(128);
                        writeBuffer.put(bytes);
                        writeBuffer.flip();
                        socketChannel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer attachment) {
                                if (attachment.hasRemaining()) {
                                    socketChannel.write(attachment, attachment, this);
                                }
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {
                                try {
                                    socketChannel.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void failed(Throwable exc, AsyncServerHandler attachment) {

            }
        });
    }
}
