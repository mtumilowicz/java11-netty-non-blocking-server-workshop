package pro;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by mtumilowicz on 2019-06-23.
 */
public class TestServerMultiThreadSocket {

    public static void main(String args[]) throws IOException {
        final int portNumber = 81;
        System.out.println("Creating server socket on port " + portNumber);
        ServerSocket serverSocket = new ServerSocket(portNumber);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Accepted connection from " + socket);
            new Thread(() -> {
                try {
                    OutputStream os = socket.getOutputStream();
                    PrintWriter pw = new PrintWriter(os, true);
                    pw.println("What's you name?");

                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String str = br.readLine();

                    pw.println("Hello, " + str);
                    pw.close();
                    socket.close();

                    System.out.println("Just said hello to:" + str);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                    }
                }
            }).start();
        }
    }
}
