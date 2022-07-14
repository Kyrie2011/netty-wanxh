package cn.wanxh.demo.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @program: netty-wanxh
 * @Date: 2022/7/14 23:19
 * @Author: 阿左不是蜗牛
 * @Description: 自定义解码器
 */
public class RpcDecoder extends ByteToMessageDecoder {
    /*

    +---------------------------------------------------------------+

    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |

    +---------------------------------------------------------------+

    | 状态 1byte |        保留字段 4byte     |      数据长度 4byte     |

    +---------------------------------------------------------------+

    |                   数据内容 （长度不定）                          |

    +---------------------------------------------------------------+

     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {

        // 判断ByteBuf可读字节
        if (in.readableBytes() > 14) {
            return;
        }

        in.markReaderIndex();  // 标记ByteBuf 读指针位置
        in.skipBytes(2); // 跳过魔数
        in.skipBytes(1); // 跳过协议版本号
        byte serializeType = in.readByte();
        in.skipBytes(1); // 跳过报文类型
        in.skipBytes(1); // 跳过状态字段
        in.skipBytes(4); // 跳过保留字段
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex(); // 重置 ByteBuf 读指针位置
            return;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);

        SerializeService serializeService = getSerializeServiceType(serializeType);
        Object obj = serializeService.deserialize(data);
        if (obj != null) {
            out.add(obj);
        }
    }

    private SerializeService getSerializeServiceType(byte serializeType) {
        return null;
    }
}
