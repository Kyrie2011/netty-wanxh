package cn.wanxh.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/6 0:06
 * @Author: 阿左不是蜗牛
 * @Description: 单线程基本处理器：IO的读写以及业务的处理
 */
public class BasicHandler implements  Runnable{

    public SocketChannel socketChannel;

    public SelectionKey selectionKey;

    public BasicHandler(SocketChannel socketChannel, Selector selector) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false); // 设置成非阻塞

        // 将该通道注册到选择器上，并关注Read事件
        this.selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        // 事件回调
        selectionKey.attach(this);
    }

    @Override
    public void run() {

    }


}
