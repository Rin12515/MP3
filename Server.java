import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

public class Server{
    public static void main(String[] args){

        if (args.length != 2){
            System.out.println("Usage: java Server <port> <max_clients>");
            return;
        }
        int port;
        int maxclients;

        try{
            port = Integer.parseInt(args[0]);
            maxclients = Integer.parseInt(args[1]);
        } catch (NumberFormatException e){
            System.out.println("Invalid port or max_clients value.");
            return;
        }
        if (port <1 || port > 65535){
            System.out.println("Port number must be between 1 and 65535.");
            return;
        }
        if (maxclients <= 0){
            System.out.println("Max clients must be a positive integer.");
            return;
        }
        try (BufferedReader keyboard = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8))){
            
            System.out.print("Enter a string: ");
            String inputString = keyboard.readLine();

            if (inputString == null){
                System.out.println("No input provided.");
                return;
            }
            int repeatCount = readPositiveInteger(keyboard);

            Semaphore availableSlots = new Semaphore(maxclients);
            try (ServerSocket serverSocket = new ServerSocket(port)){
                System.out.println("server started on port " + port);
                System.out.println("Max clients:" + maxclients);
                System.out.println("Waiting for clients to connect...");

                while (true){
                    Socket clientSocket = serverSocket.accept();
                     if(!availableSlots.tryAcquire()){
                        System.out.println("Server is busy. Rejecting your connnection "
                         + clientSocket.getRemoteSocketAddress());

                         clientSocket.close();
                         continue;
                     }
                     System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                     ClientHandler clientHandler = new ClientHandler(clientSocket,
                        inputString, repeatCount, availableSlots);
                     Thread clientThread = new Thread(clientHandler);
                     clientThread.start();
                }
            }catch (IOException e){
                System.out.println("Error starting server: " + e.getMessage());
            }

        }catch (IOException e) {
            System.err.println("Input error: " + e.getMessage());
            }
    }
    private static int readPositiveInteger(BufferedReader keyboard) throws IOException{
        while (true){
            System.out.println("Enter a number of transmissions (N): ");
            String input = keyboard.readLine();

            if (input == null){
                throw new EOFException("No transmission count provided.");
                
            }
            try{
                int number = Integer.parseInt(input.trim());
                if (number > 0){
                    return number;
                }
               
            } catch (NumberFormatException e){
                System.out.println("Invalid input. Please enter a valid integer.");
            }

        }
    }
    private static class ClientHandler implements Runnable{
        private final Socket clientSocket;
        private final String inputString;
        private final int repeatCount;
        private final Semaphore availableSlots;

        public ClientHandler(Socket clientSocket, String inputString, int repeatCount, Semaphore availableSlots){
            this.clientSocket = clientSocket;
            this.inputString = inputString;
            this.repeatCount = repeatCount;
            this.availableSlots = availableSlots;

        }
        @Override 
        public void run(){
            String clientAddress = clientSocket.getRemoteSocketAddress().toString();

            try (Socket connection = clientSocket; BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))){
                
                    for (int i = 0; i < repeatCount; i++){
                        writer.write(inputString);

                        if (i == repeatCount - 1){
                            writer.write('\n');
                        }
                        writer.flush();

                        System.out.println("Sent transmission " + (i +1) + " to client " + clientAddress);

                        if(i < repeatCount -1){
                            Thread.sleep(1000);
                        }

                    }
                    System.out.println("All transmissions sent to client " + clientAddress);
                }catch (IOException e){
                    System.err.println("There was an error communicating with client " + clientAddress + ": " + e.getMessage());
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Client thread was interrupted: " + clientAddress );
                }
                finally{
                    availableSlots.release();
                    System.out.println("Client disconnected: " + clientAddress);
                }
        }
    }
}