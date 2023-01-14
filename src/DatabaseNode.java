import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class DatabaseNode {
    private ServerSocket socket;
    private Queue<String> toConnect;
    private DatabaseRecord record;
    private int port;

    public DatabaseNode() {
        toConnect = new LinkedList<>();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setRecord(String key, String value) {
        record = new DatabaseRecord(Integer.parseInt(key),Integer.parseInt(value));
    }

    public void addToConnect(String address) {
        toConnect.add(address);
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            DatabaseNode databaseNode = new DatabaseNode();
            for (int i = 0; i < args.length; i += 2) {
                switch (args[i]) {
                    case "-tcpport" -> databaseNode.setPort(Integer.parseInt(args[i + 1]));
                    case "-record" -> {
                        String values[] = args[i + 1].trim().split(":");
                        databaseNode.setRecord(values[0], values[1]);
                    }
                    case "-connect" -> databaseNode.addToConnect(args[i + 1]);
                }
            }
            //ToDo start listening
        } else {
            System.out.println("Wrong input!");
            throw new IllegalArgumentException();
        }
    }
}
