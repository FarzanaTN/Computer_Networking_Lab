import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final int PORT = 5000;
    private static final int ROUNDS = 10;
    private static final String MODE = "RENO"; // Change to "RENO" to TAHOE switch modes

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        int cwnd = 1;
        int ssthresh = 8;
        int dupACKcount = 0;
        String lastACK = "";
        int pktCounter = 1; // match sample which starts from pkt1

        System.out.println("== TCP " + MODE.toUpperCase() + " Mode ==");

        for (int round = 1; round <= ROUNDS; round++) {
            System.out.println("Round " + round + ": cwnd = " + cwnd + ", ssthresh = " + ssthresh);
            List<String> sentPackets = new ArrayList<>();

            for (int i = 0; i < cwnd; i++) {
                sentPackets.add("pkt" + pktCounter++);
            }

            String msg = String.join(",", sentPackets);
            System.out.println("Sent packets: " + msg);
            out.println(msg);

            int ackResponses = cwnd + 3; // Allow extra responses in case of dup ACKs
            dupACKcount = 0;

            for (int i = 0; i < ackResponses; i++) {
                String ack = in.readLine();
                if (ack == null) break;

                System.out.println("Received: " + ack);

                if (ack.equals(lastACK)) {
                    dupACKcount++;
                } else {
                    dupACKcount = 1;
                    lastACK = ack;
                }

                if (dupACKcount == 3) {
                    System.out.println("==> 3 Duplicate ACKs: Fast Retransmit triggered.");
                    ssthresh = cwnd / 2;

                    if (MODE.equals("RENO")) {
                        cwnd = ssthresh;
                        System.out.println("TCP RENO Fast Recovery: cwnd -> " + cwnd);
                    } else {
                        cwnd = 1;
                        System.out.println("TCP TAHOE Reset: cwnd -> 1");
                    }

                    dupACKcount = 0;
                    break; // Finish this round early after fast retransmit
                }
            }

            // Normal growth
            if (dupACKcount < 3) {
                if (cwnd < ssthresh) {
                    cwnd *= 2;
                    System.out.println("Slow Start: cwnd -> " + cwnd);
                } else {
                    cwnd += 1;
                    System.out.println("Congestion Avoidance: cwnd -> " + cwnd);
                }
            }
        }

        socket.close();
        System.out.println("Client disconnected.");
    }
}