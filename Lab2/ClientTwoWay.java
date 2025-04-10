import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientTwoWay {
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 5001);
        System.out.println("Client Connected at server Handshaking port " + s.getPort());
        System.out.println("Client’s communication port: " + s.getLocalPort());
        System.out.println("Client is Connected.");
        System.out.println("Enter the messages that you want to send and send \"STOP\" to close the connection:");

        DataOutputStream output = new DataOutputStream(s.getOutputStream());
        DataInputStream input = new DataInputStream(s.getInputStream());
        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));

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

        s.close();
        input.close();
        output.close();
    }
}
