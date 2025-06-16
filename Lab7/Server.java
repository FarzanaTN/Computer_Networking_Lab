import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("The server started on port " + port);

        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String line;
        while ((line = in.readLine()) != null) {
            String[] packets = line.split(",");
            List<String> acks = new ArrayList<>();

            int lossIndex = new Random().nextInt(packets.length);
            String lostPacket = packets[lossIndex];

            for (int i = 0; i < packets.length; i++) {
                //if (i == lossIndex) continue;
                acks.add("ACK:" + packets[i]);
            }

            if (lossIndex == 0) {
                acks.add("ACK:NA");
                acks.add("ACK:NA");
                acks.add("ACK:NA");
            } else {
                String prevPacket = packets[lossIndex - 1];
                acks.add("ACK:" + prevPacket);
                acks.add("ACK:" + prevPacket);
                acks.add("ACK:" + prevPacket);
            }

            for (String ack : acks) {
                out.println(ack);
            }
        }

        clientSocket.close();
        serverSocket.close();
        System.out.println("Client disconnected.");
    }
}