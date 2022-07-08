package cn.wanxh.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
