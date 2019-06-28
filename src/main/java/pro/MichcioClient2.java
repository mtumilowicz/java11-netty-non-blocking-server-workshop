package pro;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by mtumilowicz on 2019-06-28.
 */
public class MichcioClient2 {

    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 81;

        try (Socket socket = new Socket(hostname, port)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println(br.readLine());

            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os, true);
            System.out.println("Type name...");
            pw.println(new Scanner(System.in).nextLine());
            
            System.out.println(br.readLine());


        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
