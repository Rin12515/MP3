import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NaiveClient {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java NaiveClient <server_ip> <server_port>");
            return;
        }

        String serverIP = args[0];
        int port;

        try {
            port = Integer.parseInt(args[1]);
            if (port < 1 || port > 65535) {
                System.out.println("Invalid port number.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            return;
        }

        try (Socket socket = new Socket(serverIP, port);
             InputStreamReader reader = new InputStreamReader(
                 socket.getInputStream(), StandardCharsets.UTF_8)) {

            int character;
            while ((character = reader.read()) != -1) {
                System.out.print((char) character);
                System.out.flush();
                if (character == '\n') {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
