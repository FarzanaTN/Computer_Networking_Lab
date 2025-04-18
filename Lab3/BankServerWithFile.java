import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BankServerWithFile {
    // File paths for persistence
    private static final String ACCOUNTS_FILE = "accounts.dat";
    private static final String TRANSACTIONS_FILE = "transactions.dat";
    
    // In-memory cache for faster access
    private static final Map<String, BankAccount> accountsCache = new ConcurrentHashMap<>();
    private static final Map<String, TransactionRecord> transactionCache = new ConcurrentHashMap<>();
    
    // Lock objects for file access
    private static final Object accountsFileLock = new Object();
    private static final Object transactionsFileLock = new Object();
    
    public static void main(String[] args) throws IOException {
        ServerSocket ss = null;
        try {
            // Initialize and load data
            initializeData();
            
            int port = 5001;
            ss = new ServerSocket(port);
            System.out.println("Bank Server started on port: " + ss.getLocalPort());
            System.out.println("Waiting for ATM connections...\n");

            // Register shutdown hook to save data before exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Saving data before shutdown...");
                try {
                    saveAccountsToFile();
                    saveTransactionsToFile();
                } catch (IOException e) {
                    System.err.println("Error saving data: " + e.getMessage());
                }
            }));

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
        } catch (Exception e) {
            System.err.println("Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (ss != null && !ss.isClosed()) {
                try {
                    ss.close();
                    System.out.println("Server socket closed");
                } catch (IOException e) {
                    System.err.println("Error closing server socket: " + e.getMessage());
                }
            }
        }
    }
    
    private static void initializeData() throws IOException {
        // Create data files if they don't exist
        File accountsFile = new File(ACCOUNTS_FILE);
        File transactionsFile = new File(TRANSACTIONS_FILE);
        
        if (!accountsFile.exists()) {
            // Create default accounts if file doesn't exist
            accountsCache.put("1234", new BankAccount("1234", "1234", 1000.0));
            accountsCache.put("5678", new BankAccount("5678", "5678", 500.0));
            accountsCache.put("1212", new BankAccount("1212", "1212", 1000.0));
            accountsCache.put("5656", new BankAccount("5656", "5656", 500.0));
            saveAccountsToFile();
            System.out.println("Created initial accounts file with test accounts");
        } else {
            loadAccountsFromFile();
        }
        
        if (!transactionsFile.exists()) {
            saveTransactionsToFile(); // Create empty transactions file
            System.out.println("Created empty transactions file");
        } else {
            loadTransactionsFromFile();
        }
    }
    
    private static void loadAccountsFromFile() throws IOException {
        synchronized (accountsFileLock) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ACCOUNTS_FILE))) {
                int count = ois.readInt();
                for (int i = 0; i < count; i++) {
                    BankAccount account = (BankAccount) ois.readObject();
                    accountsCache.put(account.getCardNumber(), account);
                }
                System.out.println("Loaded " + accountsCache.size() + " accounts from file");
            } catch (ClassNotFoundException e) {
                System.err.println("Error deserializing accounts: " + e.getMessage());
                throw new IOException("Failed to load accounts", e);
            } catch (EOFException e) {
                System.err.println("Accounts file appears to be corrupted, starting with empty accounts");
                accountsCache.clear();
            }
        }
    }
    
    private static void saveAccountsToFile() throws IOException {
        synchronized (accountsFileLock) {
            // First write to a temporary file
            File tempFile = new File(ACCOUNTS_FILE + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                oos.writeInt(accountsCache.size());
                for (BankAccount account : accountsCache.values()) {
                    oos.writeObject(account);
                }
                oos.flush();
            }
            
            // Then atomically replace the original file
            Files.move(tempFile.toPath(), new File(ACCOUNTS_FILE).toPath(), 
                       StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
    }
    
    private static void loadTransactionsFromFile() throws IOException {
        synchronized (transactionsFileLock) {
            File file = new File(TRANSACTIONS_FILE);
            if (file.length() == 0) {
                return; // Empty file
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                int count = ois.readInt();
                for (int i = 0; i < count; i++) {
                    TransactionRecord record = (TransactionRecord) ois.readObject();
                    transactionCache.put(record.getTransactionId(), record);
                }
                System.out.println("Loaded " + transactionCache.size() + " transactions from file");
            } catch (ClassNotFoundException e) {
                System.err.println("Error deserializing transactions: " + e.getMessage());
                throw new IOException("Failed to load transactions", e);
            } catch (EOFException e) {
                System.err.println("Transactions file appears to be corrupted, starting with empty transactions");
                transactionCache.clear();
            }
        }
    }
    
    private static void saveTransactionsToFile() throws IOException {
        synchronized (transactionsFileLock) {
            // First write to a temporary file
            File tempFile = new File(TRANSACTIONS_FILE + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                oos.writeInt(transactionCache.size());
                for (TransactionRecord record : transactionCache.values()) {
                    oos.writeObject(record);
                }
                oos.flush();
            }
            
            // Then atomically replace the original file
            Files.move(tempFile.toPath(), new File(TRANSACTIONS_FILE).toPath(), 
                       StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
    }
    
    private static void saveTransaction(TransactionRecord record) throws IOException {
        synchronized (transactionsFileLock) {
            transactionCache.put(record.getTransactionId(), record);
            // Save all transactions to ensure consistency
            saveTransactionsToFile();
        }
    }
    
    private static void updateAccountBalance(String cardNumber, double newBalance) throws IOException {
        synchronized (accountsFileLock) {
            BankAccount account = accountsCache.get(cardNumber);
            if (account != null) {
                account.setBalance(newBalance);
                saveAccountsToFile();
            }
        }
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
                        
                        // Check if this is a retry of a completed transaction
                        if (transactionCache.containsKey(txId)) {
                            System.out.println("Duplicate transaction detected: " + txId);
                            String cachedResponse = transactionCache.get(txId).getResponse();
                            output.writeUTF(cachedResponse + ":TX_ID:" + txId);
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
            
            BankAccount account = accountsCache.get(cardNumber);
            if (account != null && account.verifyPin(pin)) {
                currentCardNumber = cardNumber;
                isAuthenticated = true;
                sendResponse("AUTH_OK", txId);
            } else {
                sendResponse("AUTH_FAIL", txId);
            }
        }
        
        private void handleBalanceRequest(String txId) throws IOException {
            BankAccount account = accountsCache.get(currentCardNumber);
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
                BankAccount account = accountsCache.get(currentCardNumber);
                
                // Attempt withdrawal (non-idempotent operation)
                synchronized (account) {
                    if (account.getBalance() >= amount) {
                        try {
                            // First record the transaction intention
                            TransactionRecord record = new TransactionRecord(
                                txId, command, "WITHDRAW_OK", false, 
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            );
                            saveTransaction(record);
                            
                            // Now perform the actual withdrawal
                            account.withdraw(amount);
                            
                            // Update account in file
                            updateAccountBalance(currentCardNumber, account.getBalance());
                            
                            // Mark transaction as completed
                            record.setCompleted(true);
                            saveTransaction(record);
                            
                            sendResponse("WITHDRAW_OK", txId);
                        } catch (IOException e) {
                            System.err.println("File I/O error during withdrawal: " + e.getMessage());
                            e.printStackTrace();
                            sendResponse("ERROR:PERSISTENCE_ERROR", txId);
                        }
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
                try {
                    // Store the response in file
                    if (!transactionCache.containsKey(txId)) {
                        TransactionRecord record = new TransactionRecord(
                            txId, "", response, true,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        );
                        saveTransaction(record);
                    } else {
                        TransactionRecord record = transactionCache.get(txId);
                        record.setResponse(response);
                        saveTransaction(record);
                    }
                    
                    // Send the response with the transaction ID
                    output.writeUTF(response + ":TX_ID:" + txId);
                } catch (IOException e) {
                    System.err.println("File I/O error saving transaction response: " + e.getMessage());
                    e.printStackTrace();
                    output.writeUTF("ERROR:PERSISTENCE_ERROR" + (txId != null ? ":TX_ID:" + txId : ""));
                }
            } else {
                output.writeUTF(response);
            }
        }
    }
    
    static class BankAccount implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String cardNumber;
        private final String pin;
        private double balance;
        
        BankAccount(String cardNumber, String pin, double initialBalance) {
            this.cardNumber = cardNumber;
            this.pin = pin;
            this.balance = initialBalance;
        }
        
        String getCardNumber() {
            return cardNumber;
        }
        
        boolean verifyPin(String enteredPin) {
            return pin.equals(enteredPin);
        }
        
        double getBalance() {
            return balance;
        }
        
        void setBalance(double balance) {
            this.balance = balance;
        }
        
        void withdraw(double amount) {
            if (amount > 0 && balance >= amount) {
                balance -= amount;
            }
        }
    }
    
    static class TransactionRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String transactionId;
        private final String request;
        private String response;
        private boolean completed;
        private final String timestamp;
        
        TransactionRecord(String transactionId, String request, String response, boolean completed, String timestamp) {
            this.transactionId = transactionId;
            this.request = request;
            this.response = response;
            this.completed = completed;
            this.timestamp = timestamp;
        }
        
        String getTransactionId() {
            return transactionId;
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