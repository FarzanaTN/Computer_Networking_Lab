import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 5001);
        System.out.println("Client Connected at server Handshaking port " + s.getPort());

        System.out.println("Client’s communcation port " + s.getLocalPort());
        System.out.println("Client is Connected");
        System.out.println("Enter the messages that you want to send and send \"Exit\" to close the connection:");

        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));


        
        DataInputStream input = new DataInputStream(s.getInputStream());
        DataOutputStream output = new DataOutputStream(s.getOutputStream());


        // Thread to send messages from client
        Thread sendThread = new Thread(() -> {
            try {
                while (true) {
                    String str = read.readLine();
                    output.writeUTF(str);
                    if (str.equalsIgnoreCase("STOP")) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error sending message from client.");
            }
        });

        // Thread to receive messages from server
        Thread receiveThread = new Thread(() -> {
            try {
                while (true) {
                    String serverMsg = input.readUTF();
                    System.out.println("Server says: " + serverMsg);
                    if (serverMsg.equalsIgnoreCase("STOP")) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection closed or error occurred.");
            }
        });

        sendThread.start();
        receiveThread.start();

        try {
            sendThread.join();
            receiveThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        // String strOfServer = "";

     

        // String str = "";
        // while (true) {
        //     str = read.readLine();
        //     if(str.equalsIgnoreCase("exit")){
        //         output.writeUTF(str);
        //         break;
        //     }
        //     output.writeUTF(str);
        //     //added this to listen to server
        //     strOfServer = input.readUTF();
        //     System.out.println("Server says : " + strOfServer);
        // }

        output.close();
        read.close();
        s.close();
    }
}


class SendToServer extends Thread{
    
}
