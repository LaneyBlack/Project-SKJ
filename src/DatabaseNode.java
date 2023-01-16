import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class DatabaseNode {
    private ServerSocket socket;
    private ArrayList<InetSocketAddress> neighbours;
    private DatabaseRecord record;
    private int port;

    public DatabaseNode() {
        neighbours = new ArrayList<>();
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            DatabaseNode databaseNode = new DatabaseNode();
            Queue<String> toConnect = new LinkedList<>();
            for (int i = 0; i < args.length; i += 2)
                switch (args[i]) {
                    case "-tcpport" -> databaseNode.setPort(Integer.parseInt(args[i + 1]));
                    case "-record" -> {
                        String values[] = args[i + 1].trim().split(":");
                        databaseNode.setRecord(values[0], values[1]);
                    }
                    case "-connect" -> toConnect.add(args[i + 1].trim());
                    default -> System.out.println("Wrong command input!");
                }
            databaseNode.action(toConnect);
        } else {
            System.out.println("Wrong input!");
            throw new IllegalArgumentException();
        }
    }

    public void action(Queue<String> toConnect) {
        //First, connect to queued Nodes. Nodes shouldn't be unique, so no need to check for key being unique
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(), port);
            PrintWriter output;
            BufferedReader input;
            while (!toConnect.isEmpty()) {
                String[] address = toConnect.poll().split(":");
                socket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 500);
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output.println("-newnode");
                String status = input.readLine().trim();
                System.out.println("Node" + address[0] + ":" + address[1] + " -=- " + status);
                if (status.equals("welcome")) { //Status "Welcome"
                    neighbours.add(new InetSocketAddress(address[0], Integer.parseInt(address[1])));
                }
                input.close();
                output.close();
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Connecting to network exception");
            throw new RuntimeException(e);
        }
        //Second, listen to new connections
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Problem opening server");
            System.exit(-1);
        }
        System.out.println("Node listens on port " + port);
        Socket client = null;
        while (true) {
            try {
                client = socket.accept();
            } catch (IOException e) {
                System.out.println("Accept Exception");
                System.exit(-1);
            }
            (new NodeThread(client)).start();
        }
    }

    public class NodeThread extends Thread {
        private final Socket client;

        public NodeThread(Socket client) {
            super();
            this.client = client;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter output = null;
                String[] tmp = input.readLine().trim().split(" ");
                String operation, argument = null;
                if (tmp.length == 2) {
                    operation = tmp[0];
                    argument = tmp[1];
                } else
                    operation = tmp[0];
                switch (operation) {
                    case "-set-value" -> {
                        tmp = argument.split(":");
                        if (Integer.parseInt(tmp[0]) == record.getKey())
                            record.setValue(Integer.parseInt(tmp[1]));
                        else
                            for (InetSocketAddress neighbour : neighbours) {
                                //TODO check if it's not the same node if (threadSocket)
                                Socket temporary = new Socket();
                                temporary.connect(neighbour);
                                output = new PrintWriter(client.getOutputStream(), true);
                                output.println(operation + argument);
                            }
                        //ToDo wait for answer
                    }
                    case "-get-value" -> {
                        if (Integer.parseInt(tmp[0]) == record.getKey())
                            record.setValue(Integer.parseInt(tmp[1]));
                        else {

                        }
                    }
                    case "-find-value" -> {
                        //ToDo
                    }
                    case "-get-max" -> {
                        //ToDo
                    }
                    case "-get-min" -> {
                        //ToDo
                    }
                    case "-new-record" -> {
                        tmp = argument.split(":");
                        record = new DatabaseRecord(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
                        //ToDo tell client OK
                    }
                    case "-terminate" -> {
                        System.out.println("Termination");
                        for (InetSocketAddress neighbour : neighbours) {
                            client.connect(neighbour);
                            output = new PrintWriter(client.getOutputStream(), true);
                            output.println("-terminated");
                        }
                        //ToDo tell client OK
                        System.exit(0);
                    }
                    case "-terminated" -> {
                        neighbours.remove(client);
                    }
                    case "-newnode" -> {
                        //ToDo
                        neighbours.add(new InetSocketAddress(client.getLocalAddress(), client.getPort()));
                    }
                    default -> {
                        System.out.println("Wrong operation Input!");
                    }
                }
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setRecord(String key, String value) {
        record = new DatabaseRecord(Integer.parseInt(key), Integer.parseInt(value));
    }
}
