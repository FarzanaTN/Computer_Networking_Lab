// import java.io.*;
// import java.net.*;

// public class client {
//     public static void main(String[] args) throws IOException {
//         Socket s = new Socket("localhost", 5001);
//         System.out.println("Client Connected at server Handshaking port " + s.getPort());

//         System.out.println("Client’s communcation port " + s.getLocalPort());
//         System.out.println("Client is Connected");
//         System.out.println("Enter the messages that you want to send and send \"STOP\" to close the connection:");

//         BufferedReader read = new BufferedReader(new InputStreamReader(System.in));


        
//         DataInputStream input = new DataInputStream(s.getInputStream());
//         DataOutputStream output = new DataOutputStream(s.getOutputStream());


//         String strOfServer = "";

     

//         String str = "";
//         while (true) {
//             str = read.readLine();
//             if(str.equalsIgnoreCase("exit")){
//                 output.writeUTF(str);
//                 break;
//             }
//             output.writeUTF(str);
//             //addede this to listen to server
//             strOfServer = input.readUTF();
//             System.out.println("Server says : " + strOfServer);
//         }

//         output.close();
//         read.close();
//         s.close();
//     }
// }

import java.io.*;
import java.net.*;
import java.util.UUID;

public class client {
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 5001);
        DataInputStream input = new DataInputStream(s.getInputStream());
        DataOutputStream output = new DataOutputStream(s.getOutputStream());

        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Connected to bank server.");

        // Step 1: Authenticate
        System.out.println("Enter Card Number: ");
        String card = read.readLine();
        System.out.println("Enter PIN: ");
        String pin = read.readLine();

        output.writeUTF("AUTH:" + card + ":" + pin);
        String res = input.readUTF();
        System.out.println("Server: " + res);

        if (!res.equals("AUTH_OK")) {
            System.out.println("Authentication failed. Exiting.");
            s.close();
            return;
        }

        // Step 2: Request withdrawal
        while (true) {
            System.out.println("Enter amount to withdraw (or type exit): ");
            String amt = read.readLine();

            if (amt.equalsIgnoreCase("exit")) {
                output.writeUTF("exit");
                break;
            }

            String txnId = UUID.randomUUID().toString();  // unique ID per txn

            output.writeUTF("WITHDRAW:" + amt + ":" + txnId);

            String serverResponse = input.readUTF();
            System.out.println("Bank Response: " + serverResponse);

            // Step 3: Acknowledge response
            output.writeUTF("ACK");

            if (serverResponse.equals("WITHDRAW_OK")) {
                System.out.println("Collect your cash.");
            } else if (serverResponse.equals("INSUFFICIENT_FUNDS")) {
                System.out.println("Insufficient balance.");
            }
        }

        input.close();
        output.close();
        s.close();
    }
}
