package cn.wanxh.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetSocketAddress;

/**
 * HttpServer
 */
public class HttpServer {
    /**
     * 1. 服务器启动类
     *      创建引导器
     *      配置线程模型
     *      引导器绑定业务逻辑处理器
     *      配置网络参数
     *      绑定端口
     * 2. 业务逻辑处理类
     *      HttpServerHandler
     */

    public static void start(int port) throws Exception{
        /**
         * 1. 单线程Reactor模型 配置
         *  ServerBootstrap serverBootstrap = new ServerBootstrap();
         *  NioEventLoopGroup nioGroup = new NioEventLoopGroup(1);  // 指定线程数
         *  serverBootstrap.group(nioGroup);
         */

        /**
         * 2. 多线程Reactor模型 配置
         *  ServerBootstrap serverBootstrap = new ServerBootstrap();
         *  NioEventLoopGroup nioGroup = new NioEventLoopGroup();  // 默认线程数是2倍的CPU核数
         *  serverBootstrap.group(nioGroup);
         */


        /**
         * 3. 主从Reactor模型
         */
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch){
                            System.out.println(ch.config());
                            ch.pipeline()   // 每个SocketChannel创建都会build一个PipeLine与之对应，ChannelPipeline去注册多个ChannelHandle
                                    .addLast("codec", new HttpServerCodec())  // HTTP 编解码
                                    .addLast("compressor", new HttpContentCompressor())  // HttpContent 压缩
                                    .addLast("aggregator", new HttpObjectAggregator(65536))  // HTTP 消息聚合
                                    .addLast("handler", new HttpServerHandler());  // 自定义业务逻辑处理器

                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = serverBootstrap.bind().sync();
            System.out.println("Http Server started, Listening on" + port);
            f.channel().closeFuture().sync();
        }finally {
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) throws Exception {
        start(9991);   //   curl http:127.0.0.1:9991/wan
    }
}
