package cn.wanxh.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/8 0:20
 * @Author: 阿左不是蜗牛
 * @Description: 多Reactor主从模型，为了匹配CPU 和 IO的速率，可设计多个Reactor
 *  主Reactor 负责监听连接，然后将连接注册到从Reactor，将I/O转移至从Reactor
 *  从Reactor 负责通道IO的读写，处理器可选择单线程或线程池
 */
public class MultiReactor {

    private static final int POOL_SIZE = 3;

    private int port;

    int next = 0;

    // Reactor(Selector) 线程池，其中一个线程被 mainReactor 使用，剩余线程都被subReactor 使用
    static Executor selectPool = Executors.newFixedThreadPool(POOL_SIZE);

    // 主 Reactor，接收连接，把SocketChannel 注册到从 Reactor上
    private Reactor mainReactor;


    // 从 Reactor的线程，用于处理I/O读写，线程池用于处理业务逻辑
    private Reactor[] subReactors = new Reactor[POOL_SIZE -1];

    /**
     * 补充：
     *     多线程版本将业务和IO操作进行分离，Reactor线程只关注事件分发和实际的IO操作，
     *     业务处理如协议的编解码都分配给线程池处理。可能会有这样的情况发生，业务处理很快，
     *     Reactor线程大部分的都在处理IO，导致了CPU闲置，降低了响应速度。
     */
    public static void main(String[] args) {
        MultiReactor mr = new MultiReactor(10088);

    }

    public MultiReactor(int port) {
        try{
            this.port = port;
            mainReactor = new Reactor();
        }catch(Exception e){

        }

    }


    /**
     * 启动主从Reactor，初始化并注册Acceptor 到 主Reactor
     * @throws IOException
     */
    public void start() throws IOException {
        Thread mainReactorThread = new Thread(mainReactor);
        mainReactorThread.setName("mainReactor");
        // 将ServerSocketChannel 注册到 mainReactor

        selectPool.execute(mainReactorThread); // 执行主Reactor线程


    }

    /**
     * 初始化并配置 ServerSocketChannel， 注册到mainReactor 的 Selector上
     */
    class Acceptor implements  Runnable {
        final Selector sel;

        final ServerSocketChannel serverSocketChannel;

        public Acceptor(Selector selector,int port) throws IOException {
            this.sel = selector;
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            // 设置成非阻塞模式
            serverSocketChannel.configureBlocking(false);
            // 注册到选择器并设置处理socket连接事件
            SelectionKey sk = serverSocketChannel.register(sel, SelectionKey.OP_ACCEPT);
            sk.attach(this);
            System.out.println("mainReactor-" + "Acceptor: Listening on port: " + port);
        }

        @Override
        public synchronized void run() {
            try{
                // 接收连接，非阻塞模式下，没有连接直接返回 null
                SocketChannel sc = serverSocketChannel.accept();
                if (sc != null) {
                    System.out.println("mainReactor-" + "Acceptor: " + sc.socket().getLocalSocketAddress() +" 注册到 subReactor-" + next);
                    // 将接收的连接注册到从 Reactor 上
                    // 发现无法直接注册，一直获取不到锁，这是由于 从 Reactor 目前正阻塞在 select() 方法上，此方法已经
                    // 锁定了 publicKeys（已注册的key)，直接注册会造成死锁

                    // 如何解决呢，直接调用 wakeup，有可能还没有注册成功又阻塞了。这是一个多线程同步的问题，可以借助队列进行处理
                    Reactor subReactor = subReactors[next];
                    subReactor.register(new MultiThreadHandler(sc));
                    if (++next == subReactors.length) {
                        next = 0;
                    }
                }
            }catch(Exception ex){
                ex.printStackTrace();
            }

        }
    }

    static class Reactor implements Runnable {

        private ConcurrentLinkedDeque<MultiThreadHandler> events = new ConcurrentLinkedDeque<>();

        final Selector selector;

        public Reactor() throws IOException {
            selector = Selector.open();
        }

        public Selector getSelector(){
            return selector;
        }


        @Override
        public void run() {
            try{
                while (!Thread.interrupted()) {  // 死循环
                    MultiThreadHandler handler = null;
                    while ((handler = events.poll()) != null){
                        handler.socketChannel.configureBlocking(false);  // 设置非阻塞
                        handler.selectionKey = handler.socketChannel.register(selector, SelectionKey.OP_READ); // 注册通道
                        handler.selectionKey.attach(handler);
                    }

                    selector.select(); // 阻塞，直到有通道事件就绪
                    Set<SelectionKey> selected = selector.selectedKeys(); // 拿到就绪通道 SelectionKey 的集合
                    Iterator<SelectionKey> it = selected.iterator();
                    while (it.hasNext()) {
                        SelectionKey skTmp = it.next();
                        dispatch(skTmp); // 根据 key 的事件类型进行分发
                    }
                    selected.clear(); // 清空就绪通道的 key
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        /**
         * 事件分发
         * @param key
         */
        void dispatch(SelectionKey key) {
            Runnable r = (Runnable) (key.attachment()); // 拿到通道注册时附加的对象
            if (r != null) r.run();
        }

        /**
         * 将连接通道注册到从Reactor上
         * @param handler
         */
        void register(MultiThreadHandler handler){
            events.offer(handler);
            selector.wakeup();
        }
    }

}
