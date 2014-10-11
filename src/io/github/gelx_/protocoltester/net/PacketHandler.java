package io.github.gelx_.protocoltester.net;

import io.github.gelx_.protocoltester.database.DB_users;
import io.github.gelx_.protocoltester.net.Protocol.*;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

/**
 * Created by Falk on 08.10.2014.
 */
public class PacketHandler implements Runnable{

    private static final Logger LOGGER = Logger.getLogger("PacketHandler");

    private BlockingDeque<Packet> packetQueue = new LinkedBlockingDeque<>(50);
    private Thread thread;

    public PacketHandler(){
        this.thread = new Thread(this);
        thread.start();

    }

    public void queuePacket(Packet packet){
        if(!packetQueue.offer(packet)){
            LOGGER.severe("Could not handle packet: Queue overflow!");
        }
    }

    public void run(){
        while(!Thread.interrupted()){
            try {
                this.handlePacket(packetQueue.take());
            } catch (InterruptedException e) {
                LOGGER.info("PacketHandler interrupted!");
            }
        }
    }

    public void stop(){
        this.thread.interrupt();
    }

    public void handlePacket(Packet packet){

        switch (packet.getID()){
            case 1: RegisterUserPacket registerUserPacket = (RegisterUserPacket) packet;
                    DB_users registerUser = registerUserPacket.getUser();
                    System.out.println("Received registerUserPacket. User: " + registerUser.toString());
                    break;
            case 2: GetUserPacket getUserPacket = (GetUserPacket) packet;
                    String name = getUserPacket.getName();
                    System.out.println("Received getUserPacket. Name: " + name);
                    break;
            case 3:
                    System.out.println("Received getUsersPacket");
                    break;
            case 4: RespUserPacket respUserPacket = (RespUserPacket) packet;
                    DB_users respUser = respUserPacket.getUser();
                    System.out.println("Received respUserPacket. User: " + respUser.toString());
                    break;
            case 5: RespUsersPacket respUsersPacket = (RespUsersPacket) packet;
                    DB_users[] respUsers = respUsersPacket.getUsers();
                    System.out.println("Received respUsersPacket! Users: ");
                    for (DB_users respUsersUser : respUsers){
                        System.out.println(respUsersUser.toString());
                    }
                    break;
            default: LOGGER.info("No handling for packet with ID " + packet.getID() + " implemented!");
        }
    }

}
