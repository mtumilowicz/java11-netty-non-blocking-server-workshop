package pro;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by mtumilowicz on 2019-06-23.
 */
public class TestServerThreadPoolSocket {

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            100, 
            100, 
            0L,
            MILLISECONDS,
            new ArrayBlockingQueue<>(1000),
            (r, ex) -> { });

    public static void main(String args[]) throws IOException {
        final int portNumber = 81;
        System.out.println("Creating server socket on port " + portNumber);
        ServerSocket serverSocket = new ServerSocket(portNumber);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Accepted connection from " + socket);
            executor.execute(() -> {
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
            });
        }
    }
}
