package com.ls.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class TimeClient {
public static void main(String[] args) {
	int port = 8080;
	new Thread(new TimeClientNIO(port)).start();
}
}
class TimeClientNIO implements Runnable{
	private Selector selector;
	private SocketChannel sc;
	private int port = 0;
	private boolean stop;
	public TimeClientNIO(int port){
		try {
			selector = Selector.open();
			sc  = SocketChannel.open();
			sc.configureBlocking(false);
			this.port = port;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	public void run() {
		try {
			if(sc.connect(new InetSocketAddress("localhost", port))){
				sc.register(selector, SelectionKey.OP_READ);
				writeToClient(sc);
			}else{
				sc.register(selector, SelectionKey.OP_CONNECT);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SelectionKey key=null ;
		while(!isStop()){
			try{
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
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
			}catch(Exception e){
				
			}
		}
		if(selector!=null){
			try {
				selector.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	private void handlerKey(SelectionKey key) throws IOException {
		SocketChannel sc = null;
		 if (key.isValid()) {
			if( key.isConnectable()){
				
				sc = (SocketChannel)key.channel();
				sc.finishConnect();
				sc.register(selector, SelectionKey.OP_READ);
				writeToClient(sc);
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
					System.out.println("return time is : "+content);
					this.setStop(true);
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
		String time ="QUERY TIME ORDER";
		byte[] bs  = time.getBytes("utf-8");
		ByteBuffer buffer  = ByteBuffer.allocate(bs.length);
		buffer.put(bs);
		buffer.flip();
		sc.write(buffer);
	}
	public boolean isStop() {
		return stop;
	}
	public void setStop(boolean stop) {
		this.stop = stop;
	}
}