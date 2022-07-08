package cn.wanxh.reactor;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/6 0:06
 * @Author: 阿左不是蜗牛
 * @Description: 单线程基本处理器：IO的读写以及业务的处理 均在Reactor线程中完成
 */
public class BasicHandler implements  Runnable{

    private static final int MAXIN = 1024;
    private static final int MAXOUT = 1024;

    public SocketChannel socketChannel;

    public SelectionKey selectionKey;

    ByteBuffer input = ByteBuffer.allocate(MAXIN);
    ByteBuffer output = ByteBuffer.allocate(MAXOUT);

    // 定义服务的逻辑状态
    static final int READING = 0, SENDING = 1, CLOSED = 2;
    int state = READING;

    public BasicHandler(SocketChannel socketChannel, Selector selector) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false); // 设置成非阻塞

        // 将该通道注册到选择器上，并关注Read事件
        this.selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        // 将Handler作为callback对象，事件回调
        selectionKey.attach(this);

        // 唤醒 select()方法 （不是必须）
        selector.wakeup();
    }

    public BasicHandler(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        try{
            if (state == READING) {
                read();
            }else if (state == SENDING) {
                send();
            }
        }catch(Exception e){
            // 关闭连接
            try{
                socketChannel.close();
            }catch(IOException ex){

            }
        }
    }

    /**
     * 从通道读取字节
     * @throws IOException
     */
    protected void read() throws IOException {
        input.clear();
        // 从通道读取
        int n = socketChannel.read(input);

        // 如果读取了完整的数据
        if (inputIsComplete(n)){
            // 业务处理
            process();

            // 待发送的数据已经放入发送缓冲区中

            // 更改服务的逻辑状态以及要处理的事件类型
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }

    }


    // 缓存每次读取的内容
    StringBuilder request = new StringBuilder();

    /**
     * 当读取到 \r\n 时表示结束
     * @param bytes 读取的字节数，-1 通常是连接被关闭，0 非阻塞模式可能返回
     * @throws IOException
     */
    protected boolean inputIsComplete(int bytes) throws IOException {
        if (bytes > 0) {
            input.flip(); // 切换成读取模式
            while (input.hasRemaining()) {
                byte ch = input.get();

                if (ch == 3) { // ctrl+c 关闭连接
                    state = CLOSED;
                    return true;
                } else if (ch == '\r') { // continue
                } else if (ch == '\n') {
                    // 读取到了 \r\n 读取结束
                    state = SENDING;  // 更新为发送状态
                    return true;
                } else {
                    request.append((char)ch);
                }
            }
        } else if (bytes == -1) {
            // -1 客户端关闭了连接
            throw new EOFException();
        } else {} // bytes == 0 继续读取
        return false;
    }


    /**
     * 业务处理
     * @throws EOFException
     */
    protected void process() throws EOFException {
        if (state == CLOSED) {
            throw new EOFException();
        }else if (state == SENDING) {
            String requestContent = request.toString(); // 请求内容
            byte[] response = requestContent.getBytes(StandardCharsets.UTF_8);
            // 模拟输出响应
            output.put(response);
        }
    }


    protected void send() throws IOException{
        int written = -1;
        output.flip(); // 切换到读取模式，判断是否有数据要发送
        if (output.hasRemaining()) {
            written = socketChannel.write(output);
        }

        // 检查连接是否处理完毕，是否断开连接
        if (outputIsComplete(written)) {
            selectionKey.channel().close();
        }else {
            // 否则继续读取
            state = READING;
            // 把提示信息发到界面
            socketChannel.write(ByteBuffer.wrap("\r\nreactor>".getBytes()));
            selectionKey.interestOps(SelectionKey.OP_READ);
        }

    }

    /**
     * 当输入一个空行，表示连接可以关闭了
     * @param written
     * @return
     */
    private boolean outputIsComplete(int written) {
        if (written <= 0) {
            // 用户只敲了回车，断开连接
            return true;
        }

        // 清空旧数据，接着处理后续的请求
        output.clear();
        request.delete(0, request.length());
        return false;
    }

}
