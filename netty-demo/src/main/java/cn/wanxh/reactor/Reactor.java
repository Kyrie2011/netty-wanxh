package cn.wanxh.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/5 23:03
 * @Author: 阿左不是蜗牛
 * @Description: Reactor 单线程模式
 */
public class Reactor implements Runnable{
    /**
     * 1, Reactor: 负责响应IO事件，当检测到一个新的事件，将其发送给相应的Handler去处理；
     *    新的事件包含连接建立就绪、读就绪、写就绪等
     * 2, Handler: 将自身(handler) 与事件绑定，负责事件的处理，
     *    完成channel的读入，完成处理业务逻辑后，负责将结果写出channel。
     *
     * 单线程Reactor，Reactor和Handler处于一条线程执行
     */

    final Selector selector;

    final ServerSocketChannel serverSocket;

    public Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port)); // 绑定端口
        serverSocket.configureBlocking(false); // 设置成非阻塞
        // serverSocketChannel 注册到 selector上, 并关注一个事件(连接就绪)
        SelectionKey selectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        // 关联事件的处理程序 (回调函数)
        selectionKey.attach(new Acceptor());

    }


    public static void main(String[] args) {

    }


    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {

                selector.select(); // 阻塞，直到有事件到达

                // 拿到就绪通道 SelectionKey 的集合 (事件就绪的通道)
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    // 就绪事件的分发
                    dispatch(key);
                }
                selectionKeys.clear();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void dispatch(SelectionKey key){
        Runnable r = (Runnable) key.attachment();
        if (r != null){
            // 执行回调处理程序
            r.run();
        }
    }

    /**
     * 处理连接(建立)就绪事件
     */
    class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverSocket.accept();
                if (socketChannel != null) {
                    System.out.println("-----连接就绪--------");
                    /**
                     * 将socketChannel注册到selector上，并关注一个读事件，
                     * 且为该事件注册一个处理程序(类似回调函数)
                     */

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
