package pro;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by mtumilowicz on 2019-06-23.
 */
public class TestServerSocket {

    public static void main(String args[]) throws IOException {
        final int portNumber = 81;
        System.out.println("Creating server socket on port " + portNumber);
        ServerSocket serverSocket = new ServerSocket(portNumber);
        while (true) {
            Socket socket = serverSocket.accept();
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os, true);
            pw.println("What's you name?");

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String str = br.readLine();

            pw.println("Hello, " + str);
            pw.close();
            socket.close();

            System.out.println("Just said hello to:" + str);
        }
    }
}
