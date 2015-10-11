package com.eagle.nio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * Created by HXJ on 2015/10/11.
 */
public class AioTimeClient {
    public static void main(String[] args) {
        new Thread(new AsyncClientHandler(), "AIO-Server").start();
    }

    static class AsyncClientHandler implements CompletionHandler<Void, AsyncClientHandler>, Runnable {

        private AsynchronousSocketChannel channel;
        private CountDownLatch latch;

        AsyncClientHandler() {
            try {
                channel = AsynchronousSocketChannel.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            latch = new CountDownLatch(1);
            channel.connect(new InetSocketAddress("127.0.0.1", 6666), this, this);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void completed(Void result, AsyncClientHandler attachment) {
            byte[] req = "QUERY TIME ORDER".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
            writeBuffer.put(req);
            writeBuffer.flip();
            channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if (attachment.hasRemaining()) {
                        channel.write(attachment, attachment, this);
                    } else {
                        ByteBuffer readBuffer = ByteBuffer.allocate(128);
                        channel.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer attachment) {
                                attachment.flip();
                                byte[] bytes = new byte[attachment.remaining()];
                                attachment.get(bytes);
                                try {
                                    String body = new String(bytes, "UTF-8");
                                    System.out.println("Now is : " + body);
                                    //latch.countDown();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {
                                try {
                                    channel.close();
                                    latch.countDown();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                        latch.countDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void failed(Throwable exc, AsyncClientHandler attachment) {
            try {
                channel.close();
                latch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

