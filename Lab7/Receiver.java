import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {
    public static void main(String[] args) throws Exception {
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Receiver started on port " + port);

        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        // 1-based packet numbers where simulated loss will happen
        Set<Integer> packetsToDrop = new HashSet<>(Arrays.asList(3, 7, 12, 16, 20, 22));
        int packetCounter = 0;

        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("END")) break;
            String[] packets = line.split(",");

            for (String packet : packets) {
                packetCounter++;

                if (packetsToDrop.contains(packetCounter)) {
                    // Simulate loss by sending 3 duplicate ACKs for previous packet
                    String prev = (packetCounter == 1) ? "NA" : "pkt" + (packetCounter - 1);
                    for (int j = 0; j < 3; j++) {
                        out.println("ACK:" + prev);
                        System.out.println("Sent: ACK:" + prev + " (Duplicate for LOST pkt" + packetCounter + ")");
                    }
                    // NO ACK for the lost packet itself
                } else {
                    out.println("ACK:" + packet);
                    System.out.println("Sent: ACK:" + packet);
                }
            }
        }

        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
        System.out.println("Receiver closed.");
    }
}

