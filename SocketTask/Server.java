/* Server Side Code */

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    public static void main(String[] args) throws IOException {
        ServerSocket ss = null;
        try {
            
            int port = 5001;
            ss = new ServerSocket(port);
            System.out.println("Server started on port: " + ss.getLocalPort());
            System.out.println("Waiting for client connections...\n");

           

            while (true) {
                try {
                    Socket socket = ss.accept();
                    System.out.println("client connected from port: " + socket.getPort());
                    
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    
                    ClientHandler ch = new ClientHandler(socket, input, output);
                    ch.start();
                } catch (IOException e) {
                    System.out.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (ss != null && !ss.isClosed()) {
                try {
                    ss.close();
                    System.out.println("Server socket closed");
                } catch (IOException e) {
                    System.err.println("Error closing server socket: " + e.getMessage());
                }
            }
        }
    }

}


class ClientHandler extends Thread {

    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;

    ClientHandler(Socket cs, DataInputStream i, DataOutputStream o) {
        this.clientSocket = cs;
        this.dis = i;
        this.dos = o;
    }

    public void run() {
        String clientString;
        String serverString = "Hello from server and client port is " + clientSocket.getPort();

        try {
            while (true) {
                clientString = dis.readUTF();

                if (clientString.equalsIgnoreCase("exit")) {
                    System.out.println("Client [" + clientSocket.getPort() + "] disconnected.");
                    break;
                }

                System.out.println("Client says at port number : " + clientSocket.getPort() + " " + clientString);

                

                // response of server
                dos.writeUTF(serverString);

            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                dis.close();
                dos.close();
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }

    }
}