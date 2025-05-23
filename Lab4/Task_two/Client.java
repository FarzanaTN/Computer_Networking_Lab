import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_URL = "http://localhost:8080";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("Choose an option:");
            System.out.println("1. Upload file");
            System.out.println("2. Download file");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (choice == 1) {
                System.out.println("Enter the full path of the file to upload : ");
                System.out.println("Note: This must be a file, not a directory.");
                String filePath = scanner.nextLine();
                uploadFile(filePath);
            } else if (choice == 2) {
                System.out.println("Enter the filename to download (e.g., example.txt):");
                String filename = scanner.nextLine();
                System.out.println("Enter the full save path including filename :");
                System.out.println("Note: This must include the filename, not just a directory.");
                String savePath = scanner.nextLine();
                downloadFile(filename, savePath);
            } else {
                System.out.println("Invalid choice. Please select 1 or 2.");
            }
        } finally {
            scanner.close();
        }
    }

    private static void uploadFile(String filePath) {
        try {
            // Validate file path
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Invalid file path: File does not exist or is not a file.");
                return;
            }

            URL url = new URL(SERVER_URL + "/upload");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("X-Filename", file.getName());

            // Send file content
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = con.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            // Handle response
            int responseCode = con.getResponseCode();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? con.getErrorStream() : con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                if (responseCode == 200) {
                    System.out.println(response.toString());
                } else if (responseCode == 400) {
                    System.out.println("Upload failed: " + response.toString());
                } else if (responseCode == 405) {
                    System.out.println("Upload failed: Method not allowed.");
                } else {
                    System.out.println("Upload failed with response code: " + responseCode);
                }
            }
        } catch (IOException e) {
            System.err.println("Error during upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void downloadFile(String filename, String savePath) {
        try {
            // Validate filename
            if (filename == null || filename.trim().isEmpty()) {
                System.out.println("Invalid filename: Filename cannot be empty.");
                return;
            }

            // Validate save path
            File saveFile = new File(savePath);
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                System.out.println("Invalid save path: Cannot create parent directories.");
                return;
            }

            URL url = new URL(SERVER_URL + "/download?filename=" + URLEncoder.encode(filename, "UTF-8"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // Handle response
            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                try (InputStream is = con.getInputStream();
                     FileOutputStream fos = new FileOutputStream(savePath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    System.out.println("File downloaded successfully to " + savePath);
                }
            } else {
                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                if (responseCode == 404) {
                    System.out.println("File not found on server: " + response.toString());
                } else if (responseCode == 400) {
                    System.out.println("Download failed: Invalid request. " + response.toString());
                } else if (responseCode == 405) {
                    System.out.println("Download failed: Method not allowed.");
                } else {
                    System.out.println("Download failed with response code: " + responseCode);
                }
            }
        } catch (IOException e) {
            System.err.println("Error during download: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
  

