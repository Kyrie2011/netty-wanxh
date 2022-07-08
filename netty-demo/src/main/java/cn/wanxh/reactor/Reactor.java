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
     * Reactor的定义：是一个或多个输入事件的处理模式，用于处理并传递给服务处理程序的服务请求。
     * 服务处理程序判断传入请求发生的事件，并将它们同步的分派给关联的请求处理程序。
     *    Reactor线程：轮询通知发生连接或IO事件的通道，并分派合适的Handler处理
     *    IO线程：执行实际的读写操作
     *    业务线程：执行应用程序的业务逻辑
     *
     * 1, Reactor: 负责分发连接、读、写事件，当检测到一个新的事件，将其发送给相应的Handler去处理；
     *    新的事件包含连接建立就绪、读就绪、写就绪等
     * 2, Handler: 将自身(handler) 与事件绑定，负责事件的处理，
     *    完成channel的读入，完成处理业务逻辑后，负责将结果写出channel。
     *
     * 单线程Reactor，Handler在Reactor线程中执行。
     * Handler处理的过程会导致Reactor变慢，此时可以将非IO操作从Reactor线程中分离(业务处理交由线程池)。
     *
     * Channels: 连接到文件、Socket等，支持非阻塞读取
     * Buffers: 类似数组的对象，可由通道直接读取或写入
     * Selectors: 通知哪组通道有事件就绪
     * SelectionKeys: 维护IO事件的状态和绑定信息
     *
     */

    final Selector selector; // 选择器

    final ServerSocketChannel serverSocket; // 服务端socket通道

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
        try{
            Thread reactor = new Thread(new Reactor(10086));
            reactor.setName("Reactor");
            reactor.start();
            reactor.join();
        }catch(Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {

                selector.select(); // 阻塞，直到有事件到达

                // 拿到所有就绪通道 SelectionKey 的集合 (所有就绪的通道)
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    // 就绪事件的分发
                    dispatch(key);  // 由于handler可能阻塞，从而导致Reactor线程在此阻塞
                    /**
                     * 单线程Reactor模式缺点补充：
                     *      当其中某个Handler阻塞时，会导致其他所有的Client的Handler
                     *      都得不到执行，并且更严重的是，Handler的阻塞也会导致整个服务不能
                     *      接收新的client请求(因为Acceptor得不到执行)。
                     *      因此，单线程Reactor模型用的比较少。仅适用于Handler中业务处理组件
                     *      快速完成的场景。
                     */
                }
                selectionKeys.clear();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void dispatch(SelectionKey key){
        Runnable r = (Runnable) key.attachment();  // 获取key关联的处理器
        if (r != null){
            // 执行处理程序
            r.run();  // handler可能阻塞 (不是Thread，仅一个Runnable对象)
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
                     * 注意：此处只是一个Runnable，并非一个线程。单线程，从头到尾只有一个Reactor线程
                     */
                    new BasicHandler(socketChannel, selector);  // IO的读写及业务处理均由该处理器完成

                    /**
                     * 也可以使用多线程处理器
                     * 将IO的读写与业务处理分离，将业务逻辑交由线程池处理
                     */
                    // new MultiThreadHandler(selector, socketChannel);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
