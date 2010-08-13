package com.google.code.http4j.client.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.http4j.client.Connection;
import com.google.code.http4j.client.Http;
import com.google.code.http4j.client.HttpHost;
import com.google.code.http4j.client.impl.utils.IOUtils;

/**
 * @author <a href="mailto:guilin.zhang@hotmail.com">Zhang, Guilin</a>
 */
public class SocketChannelConnection implements Connection {

	private static final Logger logger = LoggerFactory.getLogger(SocketChannelConnection.class);

	protected SocketChannel channel;

	protected HttpHost host;

	public SocketChannelConnection(HttpHost host) {
		this.host = host;
	}

	@Override
	public void close() {
		IOUtils.close(channel);
	}

	@Override
	public void connect() throws IOException {
		InetSocketAddress address = getSocketAddress(host);
		channel = SocketChannel.open(address);
	}

	@Override
	public HttpHost getHost() {
		return host;
	}

	@Override
	public byte[] read() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1 << 10);
		ByteBuffer extended = ByteBuffer.allocate(buffer.capacity() << 1);
		while (channel.read(buffer) == buffer.capacity()) {
			// Increasing buffer's capacity reduces the chance to get here
			extended = ensureSpace(buffer, extended);
			IOUtils.fill(buffer, extended);
		}
		return IOUtils.extract(extended.position() == 0 ? buffer : extended);
	}

	@Override
	public boolean isClosed() {
		return !channel.isOpen();
	}

	@Override
	public void write(byte[] message) throws IOException {
		logger.debug("HTTP Request >>\r\n{}", new String(message));
		ByteBuffer buffer = ByteBuffer.wrap(message);
		channel.write(buffer);
	}

	protected ByteBuffer ensureSpace(ByteBuffer src, ByteBuffer dest) {
		return dest.remaining() < src.position() ? IOUtils.extendBuffer(dest): dest;
	}

	protected InetSocketAddress getSocketAddress(HttpHost host)
			throws UnknownHostException {
		int port = host.getPort();
		port = (port < 0) ? (host.getProtocol().equalsIgnoreCase(
				Http.PROTOCOL_HTTP) ? 80 : 443) : port;
		return new InetSocketAddress(host.getInetAddress(), port);
	}
}