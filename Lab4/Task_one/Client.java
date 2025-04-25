import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // 10.33.28.18
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        BufferedReader read = null;

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            read = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("List of available Files in server : ");
            String line;
            while ((line = read.readLine()) != null) {

                if (line.equals("Enter the file name you want to download:")) {
                    System.out.println();
                    break;
                }
                System.out.println(line);
            }

            // System.out.println("print koro");
            // Step 3: Send filename
            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Type 'exit' to exit");
                String fileName = keyboard.readLine();

                if (fileName.equalsIgnoreCase("exit")) {
                    dos.writeBytes("exit" + '\n');
                    System.out.println("Exiting...");
                    break;
                }

                
                dos.writeBytes(fileName + "\n"); // To match BufferedReader.readLine on server

                // Step 4: Read server response
                String response = read.readLine();
                if (response != null && response.trim().equals("File Found")) {
                    System.out.println("File found. Starting download...");

                    // a) Read file size
                    long fileSize = dis.readLong();
                    System.out.println("File size is : " + fileSize + "byte");

                    System.out.println("File found, Download started!");

                    // b) Prepare to save the file
                    FileOutputStream fos = new FileOutputStream("downloaded_" + fileName);

                    // c) Read file contents
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long remaining = fileSize;
                    while (remaining > 0
                            && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }

                    System.out.println("File downloaded successfully as: downloaded_" + fileName);
                    dos.writeBytes("File Successfully downloaded by port : " + socket.getPort() + "\n");
                    dos.flush();
                    System.out.println("Client side ack sent");

                    fos.close();
                    

                } else {
                    System.out.println("File_Not_Found!!");
                }
            }

        } catch (Exception e) {
            System.out.println("error is ");
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
                read.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
