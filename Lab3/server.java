/* Server Side Code */

import java.io.*;
import java.net.*;
import java.util.*;

public class server {

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(5001);
        System.out.println("Server is connected at port no: " + ss.getLocalPort());
        System.out.println("Server is connecting\n");
        System.out.println("Waiting for the client\n");

        String str = "";

        // BufferedReader read = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            // accept client connection
            Socket s = ss.accept();
            System.out.println("Client request is accepted at port no: " + s.getPort());
            System.out.println("Server’s Communication Port: " + s.getLocalPort());

            // input output stream
            DataInputStream input = new DataInputStream(s.getInputStream());
            DataOutputStream output = new DataOutputStream(s.getOutputStream());

            ClientHandler ch = new ClientHandler(s, input, output);
            ch.start();

            if (str.equalsIgnoreCase("STOP")) {
                break;
            }

        }

        ss.close();

    }

}

// class ClientHandler extends Thread {

//     private Socket clientSocket;
//     private DataInputStream dis;
//     private DataOutputStream dos;

//     ClientHandler(Socket cs, DataInputStream i, DataOutputStream o) {
//         this.clientSocket = cs;
//         this.dis = i;
//         this.dos = o;
//     }

//     public void run() {
//         String clientString;
//         String serverString = "Hello from server and client port is " + clientSocket.getPort();

//         try {
//             while (true) {
//                 clientString = dis.readUTF();

//                 if (clientString.equalsIgnoreCase("exit")) {
//                     System.out.println("Client [" + clientSocket.getPort() + "] disconnected.");
//                     break;
//                 }

//                 System.out.println("Client says at port number : " + clientSocket.getPort() + " " + clientString);

                

//                 // response of server
//                 dos.writeUTF(serverString);

//             }
//         } catch (Exception e) {
//             System.out.println(e);
//         } finally {
//             try {
//                 dis.close();
//                 dos.close();
//                 clientSocket.close();
//             } catch (IOException e) {
//                 System.out.println("Error closing resources: " + e.getMessage());
//             }
//         }

//     }
// }



class ClientHandler extends Thread {
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private static Map<String, String> txnLog = new HashMap<>();  // <txnId, response>

    ClientHandler(Socket cs, DataInputStream i, DataOutputStream o) {
        this.clientSocket = cs;
        this.dis = i;
        this.dos = o;
    }

    private boolean authenticate(String card, String pin) {
        // For simplicity, always return true
        return card.equals("1234") && pin.equals("0000");
    }

    private int getBalance(String card) {
        return 1000; // Hardcoded for simplicity
    }

    private boolean withdraw(String card, int amount) {
        // Always succeed if amount <= balance (1000)
        return amount <= getBalance(card);
    }

    public void run() {
        try {
            String clientMsg;
            String authCard = null;
            boolean isAuthenticated = false;

            while (true) {
                clientMsg = dis.readUTF();

                if (clientMsg.equalsIgnoreCase("exit")) {
                    System.out.println("Client [" + clientSocket.getPort() + "] disconnected.");
                    break;
                }

                System.out.println("Client says [" + clientSocket.getPort() + "]: " + clientMsg);

                String[] parts = clientMsg.split(":");

                switch (parts[0]) {
                    case "AUTH":
                        if (parts.length == 3 && authenticate(parts[1], parts[2])) {
                            authCard = parts[1];
                            isAuthenticated = true;
                            dos.writeUTF("AUTH_OK");
                        } else {
                            dos.writeUTF("AUTH_FAIL");
                        }
                        break;

                    case "WITHDRAW":
                        if (!isAuthenticated) {
                            dos.writeUTF("NOT_AUTHENTICATED");
                            break;
                        }

                        String amountStr = parts[1];
                        String txnId = parts[2];

                        if (txnLog.containsKey(txnId)) {
                            dos.writeUTF(txnLog.get(txnId)); // resend cached response
                        } else {
                            int amount = Integer.parseInt(amountStr);
                            String response;

                            if (withdraw(authCard, amount)) {
                                response = "WITHDRAW_OK";
                            } else {
                                response = "INSUFFICIENT_FUNDS";
                            }

                            txnLog.put(txnId, response);
                            dos.writeUTF(response);
                        }
                        break;

                    case "ACK":
                        // Optionally log that client has received message
                        break;

                    default:
                        dos.writeUTF("INVALID_COMMAND");
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                dis.close();
                dos.close();
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing: " + e.getMessage());
            }
        }
    }
}
