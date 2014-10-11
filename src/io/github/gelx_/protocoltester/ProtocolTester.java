package io.github.gelx_.protocoltester;

import io.github.gelx_.protocoltester.database.DB_users;
import io.github.gelx_.protocoltester.net.Connection;
import io.github.gelx_.protocoltester.net.Protocol;

import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * Created by Gelx on 11.10.2014.
 */
public class ProtocolTester {

    public static void main() {

        System.out.println("Startng ProtocolTester v0.1");
        Scanner input = new Scanner(System.in);

        System.out.println("Please enter hostname to connect to:");
        String hostname = input.next();
        System.out.println("Please enter the port to connect to:");
        int port = input.nextInt();

        InetSocketAddress address = new InetSocketAddress(hostname, port);

        Connection connection = new Connection(address);

        System.out.println("Connection established!");

        boolean stop = false;

        System.out.println("Please enter a packet name or \"stop\"");
        while(!stop) loop:{
            String command = input.next();

            switch (command.toLowerCase()){
                case "stop": stop = true; break;
                case "getuser":
                    System.out.println("Please enter the name of the user to query for:");
                    String name = input.next();
                    connection.queuePacketForWrite(new Protocol.GetUserPacket(address, name));
                    System.out.println("Packet queued for write!");
                    break;
                case "registeruser":
                    System.out.println("Please enter the name of the user to add:");
                    String name1 = input.next();
                    System.out.println("Please enter the mac of the user:");
                    String mac = input.next();
                    System.out.println("Please enter the time in hours till expiration date:");
                    int hours = input.nextInt();
                    long time = System.currentTimeMillis() + hours * 3600000;
                    connection.queuePacketForWrite(new Protocol.RegisterUserPacket(address, new DB_users(name1, mac, time)));
                    System.out.println("Packet queued for write!");
                    break;
                case "getusers":
                    connection.queuePacketForWrite(new Protocol.GetUsersPacket(address));
                    System.out.println("Packet queued for write!");
            }

        }
        System.out.println("Goodbye");
        System.exit(0);
    }
}
