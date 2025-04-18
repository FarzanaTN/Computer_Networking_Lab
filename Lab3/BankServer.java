import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class BankServer {
    // Database to store account information
    private static final Map<String, BankAccount> accounts = new ConcurrentHashMap<>();
    // Transaction log to ensure exactly-once semantics
    private static final Map<String, TransactionRecord> transactionLog = new ConcurrentHashMap<>();
    
    static {
        // Initialize with some test accounts
        accounts.put("1234", new BankAccount("1234", "1234", 1000.0));
        accounts.put("0987654321", new BankAccount("0987654321", "4321", 500.0));
    }

    public static void main(String[] args) throws IOException {
        int port = 5001;
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Bank Server started on port: " + ss.getLocalPort());
        System.out.println("Waiting for ATM connections...\n");

        while (true) {
            try {
                Socket socket = ss.accept();
                System.out.println("ATM connected from port: " + socket.getPort());
                
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                
                ATMSessionHandler handler = new ATMSessionHandler(socket, input, output);
                handler.start();
            } catch (IOException e) {
                System.out.println("Error accepting connection: " + e.getMessage());
            }
        }
        //ss.close();
    }
    
    static class ATMSessionHandler extends Thread {
        private final Socket socket;
        private final DataInputStream input;
        private final DataOutputStream output;
        private String currentCardNumber = null;
        private boolean isAuthenticated = false;
        
        ATMSessionHandler(Socket socket, DataInputStream input, DataOutputStream output) {
            this.socket = socket;
            this.input = input;
            this.output = output;
        }
        
        @Override
        public void run() {
            try {
                while (true) {
                    String message = input.readUTF();
                    System.out.println("Received from ATM [" + socket.getPort() + "]: " + message);
                    
                    // Extract transaction ID if present
                    String txId = null;
                    String command;
                    
                    if (message.contains(":TX_ID:")) {
                        String[] parts = message.split(":TX_ID:");
                        command = parts[0];
                        txId = parts[1];
                        System.out.println("found in cache!!");
                        
                        // Check if this is a retry of a completed transaction
                        if (transactionLog.containsKey(txId)) {
                            System.out.println("Duplicate transaction detected: " + txId);
                            String cachedResponse = transactionLog.get(txId).getResponse();
                            output.writeUTF(cachedResponse);
                            continue;
                        }
                    } else {
                        command = message;
                    }
                    
                    // Process the command
                    if (command.startsWith("AUTH:")) {
                        handleAuthentication(command, txId);
                    } else if (command.startsWith("BALANCE_REQ")) {
                        if (!isAuthenticated) {
                            sendResponse("ERROR:NOT_AUTHENTICATED", txId);
                        } else {
                            handleBalanceRequest(txId);
                        }
                    } else if (command.startsWith("WITHDRAW:")) {
                        if (!isAuthenticated) {
                            sendResponse("ERROR:NOT_AUTHENTICATED", txId);
                        } else {
                            handleWithdrawal(command, txId);
                        }
                    } else if (command.equals("ACK")) {
                        // Do nothing, this is just an acknowledgment
                        System.out.println("Received ACK from ATM");
                    } else if (command.equals("EXIT")) {
                        System.out.println("ATM session ended");
                        break;
                    } else {
                        sendResponse("ERROR:INVALID_COMMAND", txId);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection error with ATM: " + e.getMessage());
            } finally {
                try {
                    input.close();
                    output.close();
                    socket.close();
                    System.out.println("ATM connection closed");
                } catch (IOException e) {
                    System.out.println("Error closing resources: " + e.getMessage());
                }
            }
        }
        
        private void handleAuthentication(String command, String txId) throws IOException {
            String[] parts = command.split(":");
            if (parts.length != 3) {
                sendResponse("AUTH_FAIL", txId);
                return;
            }
            
            String cardNumber = parts[1];
            String pin = parts[2];
            
            BankAccount account = accounts.get(cardNumber);
            if (account != null && account.verifyPin(pin)) {
                currentCardNumber = cardNumber;
                isAuthenticated = true;
                sendResponse("AUTH_OK", txId);
            } else {
                sendResponse("AUTH_FAIL", txId);
            }
        }
        
        private void handleBalanceRequest(String txId) throws IOException {
            BankAccount account = accounts.get(currentCardNumber);
            double balance = account.getBalance();
            sendResponse("BALANCE_RES:" + balance, txId);
        }
        
        private void handleWithdrawal(String command, String txId) throws IOException {
            String[] parts = command.split(":");
            if (parts.length != 2) {
                sendResponse("ERROR:INVALID_COMMAND", txId);
                return;
            }
            
            try {
                double amount = Double.parseDouble(parts[1]);
                BankAccount account = accounts.get(currentCardNumber);
                
                // Attempt withdrawal (non-idempotent operation)
                synchronized (account) {
                    if (account.getBalance() >= amount) {
                        // Process might fail here - we need to log the operation intention first
                        transactionLog.put(txId, new TransactionRecord(txId, command, "WITHDRAW_OK", false));
                        
                        // Now perform the actual withdrawal
                        account.withdraw(amount);
                        
                        // Mark transaction as completed
                        transactionLog.get(txId).setCompleted(true);
                        
                        sendResponse("WITHDRAW_OK", txId);
                    } else {
                        sendResponse("INSUFFICIENT_FUNDS", txId);
                    }
                }
            } catch (NumberFormatException e) {
                sendResponse("ERROR:INVALID_AMOUNT", txId);
            }
        }
        
        private void sendResponse(String response, String txId) throws IOException {
            // If this is a transaction response that needs to be logged
            if (txId != null) {
                // Store the response for possible retries
                if (!transactionLog.containsKey(txId)) {
                    transactionLog.put(txId, new TransactionRecord(txId, "", response, true));
                } else {
                    transactionLog.get(txId).setResponse(response);
                }
                
                // Send the response with the transaction ID
                output.writeUTF(response + ":TX_ID:" + txId);
            } else {
                output.writeUTF(response);
            }
        }
    }
    
    static class BankAccount {
        private final String cardNumber;
        private final String pin;
        private double balance;
        
        BankAccount(String cardNumber, String pin, double initialBalance) {
            this.cardNumber = cardNumber;
            this.pin = pin;
            this.balance = initialBalance;
        }
        
        boolean verifyPin(String enteredPin) {
            return pin.equals(enteredPin);
        }
        
        double getBalance() {
            return balance;
        }
        
        void withdraw(double amount) {
            if (amount > 0 && balance >= amount) {
                balance -= amount;
            }
        }
    }
    
    static class TransactionRecord {
        private final String transactionId;
        private final String request;
        private String response;
        private boolean completed;
        
        TransactionRecord(String transactionId, String request, String response, boolean completed) {
            this.transactionId = transactionId;
            this.request = request;
            this.response = response;
            this.completed = completed;
        }
        
        String getResponse() {
            return response;
        }
        
        void setResponse(String response) {
            this.response = response;
        }
        
        void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }
}