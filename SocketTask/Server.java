/* Server Side Code */

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    static ServerSocket ss = null;

    public static ArrayList<ClientHandler> allClients = new ArrayList<>();

    public static void main(String[] args) throws IOException {

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
                    allClients.add(ch);
                    ch.start();

                } catch (IOException e) {
                    System.out.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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

    public static synchronized void removeClients(ClientHandler ch) {
        allClients.remove(ch);
        System.out.println("Total Active clients : " + allClients.size());
    }

}

class ClientHandler extends Thread {

    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    boolean isRun = false;

    ClientHandler(Socket cs, DataInputStream i, DataOutputStream o) {
        this.clientSocket = cs;
        this.dis = i;
        this.dos = o;
    }

    public void run() {
        String readClient;

        // Thread serverInputThread = new Thread(() -> {

        //     if (Server.allClients.isEmpty() && isRun == true) {
        //         System.out.println("No active clients. Server shutting down...");
        //         try {
        //             Server.ss.close();
        //         } catch (IOException e) {
        //             e.printStackTrace();
        //         }
        //         System.exit(0);
        //     } else {
        //         System.out.println("There are still active clients connected. Can't stopserver yet.");
        //     }

        // });
        // serverInputThread.start();

        try {
            while (true) {

                readClient = dis.readUTF();
                System.out.println("Client says at port number " + clientSocket.getPort() + " " + readClient);
                if (readClient.equalsIgnoreCase("stop")) {
                    System.out.println("Client disconnetced at port " + clientSocket.getPort());
                    Server.removeClients(this);
                    break;
                }

                BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
                String strServer = read.readLine();
                isRun = true;
                //System.out.println(isRun);

                
                dos.writeUTF(strServer);
             
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
