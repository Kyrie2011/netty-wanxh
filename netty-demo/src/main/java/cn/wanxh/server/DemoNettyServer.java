package cn.wanxh.server;

import cn.wanxh.demo.hander.MyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.SocketChannel;

/**
 * wanxh
 */
public class DemoNettyServer {


    public static void main(String[] args) {
        // 创建两个线程组 bossGroup、workerGroup
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            // 创建服务端的启动对象，设置参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 设置两个线程组bossGroup和workGroup
            bootstrap.group(bossGroup, workGroup)
                    // 设置服务端通道实现类型
                    .channel(NioSctpServerChannel.class)
                    // 设置线程队列最大连接个数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 设置保持活动连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                    // 使用匿名内部类的形式初始化通道对象
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 给pipeline管道设置处理器
                            socketChannel.pipeline().addLast(new MyServerHandler());
                        }
                    }); // 给workerGroup的EventLoop对应的管道设置处理器
            System.out.println("服务端已经准备就绪......");
            // 绑定端口号，启动服务端

            ChannelFuture channelFuture = bootstrap.bind(8888).sync();
            channelFuture.channel().closeFuture().sync();  // ?
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭线程资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }


    }

}
