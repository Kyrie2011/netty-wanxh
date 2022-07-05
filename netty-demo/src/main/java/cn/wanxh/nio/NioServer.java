package cn.wanxh.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioServer {

    public static void main(String[] args) throws IOException {

        testServer();

    }


    public static void testServer() throws IOException{

        // 1. 获取Selector选择器
        Selector selector = Selector.open();

        // 2. 获取ServerSocket的通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        // 3. 设置为非阻塞
        serverSocketChannel.configureBlocking(false);

        // 4. 绑定连接
        serverSocketChannel.bind(new InetSocketAddress(19999));

        // 5. 将通道注册到选择器上，并注册的操作为：“接收”操作
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 6. 采用轮询的方式，查询获取“准备就绪”的通道
        while (selector.select() > 0){
            // 7. 获取当前选择器中所有注册的选择键、已经准备就绪的操作
            Iterator<SelectionKey> selectKeys = selector.selectedKeys().iterator();
            while (selectKeys.hasNext()){
                // 8. 获取准备就绪的事件key
                SelectionKey selectionKey = selectKeys.next();

                // 9. 判断key是具体的什么事件
                if (selectionKey.isAcceptable()){
                    // 10. 接收事件, 获取“数据”socket连接给客户端
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    // 11. 切换为非阻塞模式
                    socketChannel.configureBlocking(false);
                    // 12. 将该通道注册到Selector选择器上, 注册感谢兴趣的事件为Read事件
                    socketChannel.register(selector, SelectionKey.OP_READ);

                }else if (selectionKey.isReadable()){
                    // 13. 读事件，获取该选择器上的“读就绪”状态的通道
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

                    // 14. 读取数据
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int length = 0;
                    while ((length = socketChannel.read(byteBuffer)) != -1) {
                        // 切换为读模式
                        byteBuffer.flip();
                        System.out.println(new String(byteBuffer.array(), 0, length));
                        byteBuffer.clear();

                    }
                    socketChannel.close();
                    // 15. 移除选择键
                    selectKeys.remove();

                }

            }
        }

        // 关闭连接
        serverSocketChannel.close();

    }

}
