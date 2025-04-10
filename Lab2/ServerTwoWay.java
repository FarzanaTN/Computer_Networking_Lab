/* Server Side Code */
import java.io.BufferedReader;
import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class ServerTwoWay {

    public static boolean isPrime(int x) {
        if (x <= 1) {
            return false;
        }

        for (int i = 2; i <= Math.sqrt(x); i++) {
            if (x % i == 0) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPalindrome(int x) {

        if (x < 0) {
            return false;
        }

        int original = x;
        int reversed = 0;

        while (x != 0) {
            int digit = x % 10;
            reversed = reversed * 10 + digit;
            x /= 10;
        }

        return original == reversed;
    }

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(5001);
        System.out.println("Server is connected at port no: " + ss.getLocalPort());
        System.out.println("Server is connecting\n");
        System.out.println("Waiting for the client...\n");
        Socket s = ss.accept();
        System.out.println("Client request is accepted at port no: " + s.getPort());
        System.out.println("Server’s Communication Port: " + s.getLocalPort());
    
        DataInputStream input = new DataInputStream(s.getInputStream());
        DataOutputStream output = new DataOutputStream(s.getOutputStream());
        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
    
        // Thread for reading from client
        Thread readThread = new Thread(() -> {
            try {
                String str;
                while (true) {
                    str = input.readUTF();
                    System.out.println("Client Says: " + str);
    
                    if (str.equalsIgnoreCase("STOP")) {
                        System.out.println("Client has disconnected.");
                        break;
                    }
    
                    String[] parts = str.split(" ", 2);
                    String serverResponse;
    
                    if (parts.length == 2) {
                        int number = Integer.parseInt(parts[0]);
                        String message = parts[1];
    
                        if (message.equalsIgnoreCase("PRIME")) {
                            serverResponse = isPrime(number) ? "Prime Number" : "Not Prime Number";
                        } else if (message.equalsIgnoreCase("PALINDROME")) {
                            serverResponse = isPalindrome(number) ? "Palindrome" : "Not Palindrome";
                        } else {
                            serverResponse = "Unknown Command";
                        }
    
                        output.writeUTF(serverResponse);
                    } else {
                        serverResponse = str.toLowerCase();
                        output.writeUTF(serverResponse);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection closed or error occurred.");
            }
        });
    
        // Thread for server to send messages
        Thread writeThread = new Thread(() -> {
            try {
                while (true) {
                    String serverMsg = read.readLine();
                    if (serverMsg.equalsIgnoreCase("STOP")) {
                        output.writeUTF("STOP");
                        break;
                    }
                    output.writeUTF(serverMsg);
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
    
        s.close();
        ss.close();
        input.close();
    }
     

   
}