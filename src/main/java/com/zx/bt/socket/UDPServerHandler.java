package com.zx.bt.socket;

import com.zx.bt.config.Config;
import com.zx.bt.socket.processor.UDPProcessorManager;
import com.zx.bt.util.Bencode;
import com.zx.bt.util.SendUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * author:ZhengXing
 * datetime:2018-02-13 12:26
 * dht服务端处理类
 */
@Slf4j
@ChannelHandler.Sharable//此处,该注解是为了重启时,不会报错,而非该对象可以被复用
public class UDPServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private static final String LOG = "[DHT服务端处理类]-";

	//当前处理器针对的nodeId索引
	private final int index;

	private final Bencode bencode;
	private final Config config;
	private final UDPProcessorManager udpProcessorManager;
	private final ProcessQueue processQueue;



	public UDPServerHandler(int index,Bencode bencode, Config config, UDPProcessorManager udpProcessorManager,
							ProcessQueue processQueue) {
		this.index = index;
		this.bencode = bencode;
		this.config = config;
		this.udpProcessorManager = udpProcessorManager;
		this.processQueue = processQueue;
	}


	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("{}通道激活", LOG);
		//给发送器工具类的channel赋值
		SendUtil.setChannel(ctx.channel(),this.index);
	}


	/**
	 * 接收到消息
	 */
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		byte[] bytes = getBytes(packet);
		InetSocketAddress sender = packet.sender();

		//责任链处理
		processQueue.put(new ProcessQueue.A(bytes,sender,index));
	}

	/**
	 * ByteBuf -> byte[]
	 */
	private byte[] getBytes(DatagramPacket packet) {
		//读取消息到byte[]
		ByteBuf byteBuf = packet.content();
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(bytes);
		return bytes;
	}

	/**
	 * 异常捕获
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("{}发生异常:{}", LOG, cause.getMessage(),cause);
		//这个巨坑..发生异常(包括我自己抛出来的)后,就关闭了连接,..
//        ctx.close();
	}
}
