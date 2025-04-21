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
        //String serverString = "Hello from server and client port is " + clientSocket.getPort();

        // Thread for reading from client
        Thread readThread = new Thread(() -> {
            try {
                String str;
                while (true) {
                    str = dis.readUTF();
                    System.out.println("Client Says:(port number : " + clientSocket.getPort() + ") :" + str);
    
                    if (str.equalsIgnoreCase("STOP")) {
                        System.out.println("Client has disconnected.");
                        break;
                    }
    
                   
                }
            } catch (IOException e) {
                System.out.println("Connection closed or error occurred.");
            }
        });
    
        // Thread for server to send messages
        Thread writeThread = new Thread(() -> {
            BufferedReader read = new BufferedReader(new InputStreamReader(System.in));

            try {
                while (true) {
                    String serverMsg = read.readLine();
                    // if ( serverMsg.equalsIgnoreCase("STOP")) {
                    //     dos.writeUTF("STOP");
                    //     break;
                    // }
                    dos.writeUTF(serverMsg);
                }
            } catch (IOException e) {
                System.out.println("Error sending message from server.");
            }
        });
    
        // Start both threads
        readThread.start();
        writeThread.start();
    
        try {
            readThread.join();
            writeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}

