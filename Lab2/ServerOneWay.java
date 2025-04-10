
/* Server Side Code */
import java.io.BufferedReader;
import java.io.DataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class ServerOneWay {

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
        System.out.println("Waiting for the client\n");
        Socket s = ss.accept();
        System.out.println("Client request is accepted at port no: " + s.getPort());
        System.out.println("Server’s Communication Port: " + s.getLocalPort());
        DataInputStream input = new DataInputStream(s.getInputStream());
        String str = "";

        DataOutputStream output = new DataOutputStream(s.getOutputStream());
        BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
        String serverString = "";

        while (true) {
            str = input.readUTF();
            System.out.println("Client Says: " + str);

            String[] parts = str.split(" ", 2);
            if (parts.length == 2) {
                int number = Integer.parseInt(parts[0]);
                String message = parts[1];

                System.out.println("Number: " + number);
                System.out.println("Message: " + message);
                if (message.equals("PRIME")) {
                    serverString = isPrime(number) ? "Prime Number" : "Not Prime Number";
                    output.writeUTF(serverString);
                } else if (message.equals("PALINDROME")) {

                    serverString = isPalindrome(number) ? "Palindrome" : "Not Palindrome";
                    output.writeUTF(serverString);
                }
            } else if (!str.equals("")) {

                serverString = str.toLowerCase();// convert capital to small from client and

                output.writeUTF(serverString);
            }

            // added this to send msg from server
            else {
                serverString = read.readLine();
                output.writeUTF(serverString);
            }

            if (str.equals("STOP")) {
                break;
            }
            
        }
        s.close();
        ss.close();
        input.close();
    }

}