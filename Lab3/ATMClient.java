import java.io.*;
import java.net.*;
import java.util.*;

public class ATMClient {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    
    // Store pending transaction states
    private static final Map<String, TransactionState> pendingTransactions = new HashMap<>();
    
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5001);
            System.out.println("ATM connected to Bank Server on port: " + socket.getPort());
            System.out.println("ATM local port: " + socket.getLocalPort());
            
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            
            // Start response listener thread
            ResponseListener responseListener = new ResponseListener(socket, input);
            responseListener.start();
            
            System.out.println("\n=== ATM Interface ===");
            System.out.println("Available commands:");
            System.out.println("1. AUTH:<card_number>:<pin>");
            System.out.println("2. BALANCE_REQ");
            System.out.println("3. WITHDRAW:<amount>");
            System.out.println("4. EXIT");
            
            String command;
            while (true) {
                System.out.print("\nEnter command: ");
                command = consoleReader.readLine();
                
                if (command.equalsIgnoreCase("EXIT")) {
                    output.writeUTF("EXIT");
                    break;
                }
                
                // Generate a unique transaction ID for non-idempotent operations
                String txId = null;
                if (command.startsWith("AUTH:") || command.startsWith("WITHDRAW:") || command.equals("BALANCE_REQ")) {
                    txId = UUID.randomUUID().toString();
                    String commandWithTxId = command + ":TX_ID:" + txId;
                    
                    // Create transaction state
                    TransactionState txState = new TransactionState(command, 0);
                    pendingTransactions.put(txId, txState);
                    
                    // Send with retry logic
                    sendWithRetry(output, commandWithTxId, txId);
                } else {
                    output.writeUTF(command);
                }
            }
            
            responseListener.interrupt();
            socket.close();
            System.out.println("ATM disconnected");
            
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
    
    private static void sendWithRetry(DataOutputStream output, String message, String txId) {
        TransactionState txState = pendingTransactions.get(txId);
        Thread retryThread = new Thread(() -> {
            try {
                // Initial send
                output.writeUTF(message);
                System.out.println("Sent: " + message);
                
                // Wait for response or retry
                while (!txState.isCompleted() && txState.getRetryCount() < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                    
                    if (!txState.isCompleted()) {
                        txState.incrementRetry();
                        System.out.println("No response received. Retrying (" + txState.getRetryCount() + "/" + MAX_RETRIES + ")");
                        output.writeUTF(message);
                    }
                }
                
                if (!txState.isCompleted() && txState.getRetryCount() >= MAX_RETRIES) {
                    System.out.println("Transaction failed after " + MAX_RETRIES + " attempts.");
                    pendingTransactions.remove(txId);
                }
                
            } catch (IOException | InterruptedException e) {
                System.out.println("Error in retry thread: " + e.getMessage());
            }
        });
        
        retryThread.start();
    }
    
    static class ResponseListener extends Thread {
        private final Socket socket;
        private final DataInputStream input;
        
        ResponseListener(Socket socket, DataInputStream input) {
            this.socket = socket;
            this.input = input;
        }
        
        @Override
        public void run() {
            try {
                while (!isInterrupted() && socket.isConnected()) {
                    String response = input.readUTF();
                    
                    // Check if response contains transaction ID
                    String txId = null;
                    String actualResponse;
                    
                    if (response.contains(":TX_ID:")) {
                        String[] parts = response.split(":TX_ID:");
                        actualResponse = parts[0];
                        txId = parts[1];
                        
                        // Mark transaction as completed
                        if (pendingTransactions.containsKey(txId)) {
                            pendingTransactions.get(txId).setCompleted(true);
                            
                            // Send ACK for the response
                            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                            output.writeUTF("ACK");
                        }
                    } else {
                        actualResponse = response;
                    }
                    
                    System.out.println("Received: " + actualResponse);
                    
                    // Process the response
                    if (actualResponse.equals("AUTH_OK")) {
                        System.out.println("Authentication successful");
                    } else if (actualResponse.equals("AUTH_FAIL")) {
                        System.out.println("Authentication failed. Please check card number and PIN.");
                    } else if (actualResponse.startsWith("BALANCE_RES:")) {
                        System.out.println("Current balance: $" + actualResponse.split(":")[1]);
                    } else if (actualResponse.equals("WITHDRAW_OK")) {
                        System.out.println("Withdrawal successful. Please take your cash.");
                    } else if (actualResponse.equals("INSUFFICIENT_FUNDS")) {
                        System.out.println("Insufficient funds for this withdrawal.");
                    } else if (actualResponse.startsWith("ERROR:")) {
                        System.out.println("Error: " + actualResponse.substring(6));
                    }
                }
            } catch (IOException e) {
                if (!isInterrupted()) {
                    System.out.println("Connection to server lost: " + e.getMessage());
                }
            }
        }
    }
    
    static class TransactionState {
        private final String command;
        private int retryCount;
        private boolean completed;
        
        TransactionState(String command, int retryCount) {
            this.command = command;
            this.retryCount = retryCount;
            this.completed = false;
        }
        
        int getRetryCount() {
            return retryCount;
        }
        
        void incrementRetry() {
            retryCount++;
        }
        
        boolean isCompleted() {
            return completed;
        }
        
        void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }
}