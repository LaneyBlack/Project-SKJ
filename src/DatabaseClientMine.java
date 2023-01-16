import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DatabaseClientMine {
    private InetSocketAddress gateway;

    public static void main(String[] args) {
        if (args.length > 2) {
            DatabaseClientMine databaseClient = new DatabaseClientMine();
            if (args[0] == "-gateway") {
                databaseClient.setGateway(args[1]);
                if (args[2] == "-operation")
                    databaseClient.operate(args[3] + " " + args[4]);
            }else
                System.out.println("Wrong command");
        } else {
            System.out.println("Wrong input!");
            throw new IllegalArgumentException();
        }
    }

    public void operate(String operation) {
        try {
            Socket socket = new Socket();
            socket.connect(gateway, 500);
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output.println(operation);
            System.out.println("Node response: " + input.readLine());
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Connection exception");
            throw new RuntimeException(e);
        }
    }

    public void setGateway(String address) {
        String [] values = address.trim().split(":");
        gateway = new InetSocketAddress(values[0],Integer.parseInt(values[1]));
    }
}
