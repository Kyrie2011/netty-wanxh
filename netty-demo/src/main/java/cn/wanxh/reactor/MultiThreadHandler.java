package cn.wanxh.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/7 22:35
 * @Author: 阿左不是蜗牛
 * @Description: 多线程基本处理器：I/O读写由 Reactor 线程处理，业务的处理交给线程池
 */
public class MultiThreadHandler extends  BasicHandler{

    static Executor workPool = Executors.newFixedThreadPool(5);

    static final int PROCESSING = 4;

    private Object lock = new Object();

    public MultiThreadHandler(Selector selector, SocketChannel socketChannel) throws IOException {
        super(socketChannel, selector);
    }

    @Override
    protected void read() throws IOException {
        synchronized (lock){
            input.clear();
            int n = socketChannel.read(input);
            // 是否读取完毕
            if (inputIsComplete(n)) {
                // 读取完毕后将后续的处理交给线程池
                state = PROCESSING;
                // 使用线程池异步执行
                workPool.execute(new Processer());


            }
        }
    }

    class Processer implements Runnable{
        @Override
        public void run() {
            processAndHandOff();
        }
    }

    protected void processAndHandOff() {
        synchronized (lock) {
            try{
                process();
            }catch(Exception e){
                // 关闭连接
                try{
                    selectionKey.channel().close();
                }catch(IOException ex){
                }
                return;
            }

            // 最后的发送还是交给Reactor线程处理
            state = SENDING;
            // process完，注册事件为Write
            selectionKey.interestOps(SelectionKey.OP_WRITE);

            // 这里需要唤醒 Selector，因为当把处理交给 workPool 时，Reactor 线程已经阻塞在 select() 方法了， 注意
            // 此时该通道感兴趣的事件还是 OP_READ，这里将通道感兴趣的事件改为 OP_WRITE，如果不唤醒的话，就只能在
            // 下次select 返回时才能有响应了，当然了也可以在 select 方法上设置超时
            selectionKey.selector().wakeup();

        }
    }
}
