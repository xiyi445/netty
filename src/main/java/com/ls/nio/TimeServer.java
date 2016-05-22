package com.ls.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class TimeServer {
public static void main(String[] args) {
	int port = 8080;
	new Thread(new TimeServerNIO(port)).start();
}
}

class TimeServerNIO implements Runnable{
	private Selector select ;
	private boolean stop;
	private ServerSocketChannel channel;
	public TimeServerNIO(int port){
		try {
			select = Selector.open();
			channel =ServerSocketChannel.open();
			channel.socket().bind(new InetSocketAddress(port), 1024);
			channel.configureBlocking(false);
			channel.register(select, SelectionKey.OP_ACCEPT);  	
			System.out.println("server started ....");
		} catch (IOException e) {
		}
		
	}
	public void run() {
		SelectionKey key=null ;
		while(!isStop()){
			try {
				select.select(1000);
				Set<SelectionKey> selectedKeys = select.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				while(it.hasNext()){
					key =	it.next();
					it.remove();
					try{
					handlerKey(key);
					}catch(Exception e){
						if(key!=null){
							key.cancel();
							try {
								if(key.channel()!=null)
								key.channel().close();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			} catch (IOException e) {
			
			}
			
		}
		if(select!=null){
			try {
				select.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	private void handlerKey(SelectionKey key) throws IOException {
		SocketChannel sc = null;
		 if (key.isValid()) {
			if( key.isAcceptable()){
				channel = (ServerSocketChannel)key.channel();
				SocketChannel client = channel.accept();
				client.configureBlocking(false);
				client.register(select, SelectionKey.OP_READ);
			}
			if(key.isReadable()){
				sc = (SocketChannel)key.channel();
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				int size = sc.read(buffer);
				if(size>0){
					buffer.flip();
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					String content = new String(bytes,"utf-8");
					if(StringUtils.isNotBlank(content)&&content.equalsIgnoreCase("QUERY TIME ORDER")){
						writeToClient(sc);
					}
				}else if(size<0){
					if(sc!=null)
						sc.close();
					key.cancel();
				}else{
					;
				}
			}
		 }
		
	}
	private void writeToClient(SocketChannel sc) throws IOException {
		String time = new Date().toString();
		byte[] bs  = time.getBytes("utf-8");
		ByteBuffer buffer  = ByteBuffer.allocate(bs.length);
		buffer.put(bs);
		buffer.flip();
		sc.write(buffer);
	}
	
	public Selector getSelect() {
		return select;
	}
	public void setSelect(Selector select) {
		this.select = select;
	}
	public boolean isStop() {
		return stop;
	}
	public void setStop(boolean stop) {
		this.stop = stop;
	}
	
}