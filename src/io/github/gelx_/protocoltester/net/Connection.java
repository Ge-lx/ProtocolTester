package io.github.gelx_.protocoltester.net;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Gelx on 10.10.2014.
 */
public class Connection implements Runnable{

    private static final Logger LOGGER = Logger.getLogger("Connection");

    private Selector selector;
    private SocketChannel channel;

    private PacketHandler handler;

    private Queue<ByteBuffer> outputQueue = new LinkedList<>();
    private Thread thread;

    public Connection(InetSocketAddress address){
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            LOGGER.severe("Could not open Selector! " + e.getMessage());
            System.exit(1);
        }
        try {
            this.channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            System.out.println("Connecting...");
            while(!channel.finishConnect());
            System.out.println("Successfully connected!");
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (IOException e) {
            LOGGER.severe("Could not initialize sockets! " + e.getMessage());
        }

        handler = new PacketHandler();

        this.thread = new Thread(this);
        this.thread.start();
    }

    public void run(){
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(4); //Max 4 byte for int

        while(!Thread.interrupted()){
            try {
                if (selector.select(10 * 1000) == 0) {
                    LOGGER.severe("No Socket is ready for operations after 10sec. Aborting.");
                    try {
                        channel.close();
                    }catch(IOException e){
                        LOGGER.severe("Exception while closing socket! " + e.getMessage());
                    }
                    selector.close();
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    SocketChannel channel = (SocketChannel) key.channel();


                    if(!key.isValid()){
                        String hostname = ((InetSocketAddress)((SocketChannel)key.channel()).getRemoteAddress()).getHostString();//ridiculous
                        LOGGER.info("Connection with " + hostname  + " closed by remote host!");
                        key.channel().close();

                    }
                    if(key.isWritable()){
                        if(!outputQueue.isEmpty()){
                            channel.write(outputQueue.poll());
                            LOGGER.info("Sent packet!");
                        }
                    }else if(key.isReadable()){
                        //Read packID (short -> 2b)
                        readToBuffer(readBuffer, channel, 2);
                        short packetID = readBuffer.getShort();
                        //Read dataSize (int -> 4b)
                        readToBuffer(readBuffer, channel, 4);
                        int dataSize = readBuffer.getInt();
                        //Read data (size -> dataSize)
                        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(dataSize);
                        readToBuffer(dataBuffer, channel, dataSize);

                        //Hand off to packetHandler
                        handler.handlePacket(Protocol.unpackPacket(channel.getRemoteAddress(), packetID, dataBuffer));
                        LOGGER.info("Received packet!");
                    }

                    iterator.remove();
                }
            } catch (IOException e) {
                LOGGER.severe("Error in Connection! " + e.getMessage());
                Thread.currentThread().interrupt();
            }

        }

        LOGGER.severe("No Socket is ready for operations after 10sec. Aborting.");
        try {
            channel.close();
        }catch(IOException e){
            LOGGER.severe("Exception while closing socket! " + e.getMessage());
        }
        try {
            selector.close();
        } catch (IOException e) {
            LOGGER.severe("Exception while closing selector! " + e.getMessage());
        }

    }

    public void closeConnections(){
        thread.interrupt();
    }

    public void queuePacketForWrite(Protocol.Packet packet){
        outputQueue.add(Protocol.packPacket(packet));
    }

    private void readToBuffer(ByteBuffer buffer, SocketChannel channel, int length) throws IOException {
        buffer.clear();
        buffer.limit(length);
        while(buffer.remaining() > 0)
            channel.read(buffer);
        buffer.flip();
    }

}
