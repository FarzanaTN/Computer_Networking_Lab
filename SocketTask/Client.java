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


        


        String strOfServer = "";

     

        String str = "";
        while (true) {
            str = read.readLine();
            if(str.equalsIgnoreCase("stop")){
                output.writeUTF(str);
                break;
            }
            output.writeUTF(str);
            //added this to listen to server
            strOfServer = input.readUTF();
            System.out.println("Server says : " + strOfServer);
        }

        output.close();
        read.close();
        s.close();
    }
}

