import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;

public class DatabaseNode {
    private ServerSocket server;
    private final ArrayList<InetSocketAddress> neighbours;
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
                        String[] values = args[i + 1].trim().split(":");
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
        Socket socket = new Socket();
        PrintWriter output;
        BufferedReader input;
        while (!toConnect.isEmpty()) {
            String[] address = toConnect.poll().split(":");
            try {
                socket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 500);
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output.println("newnode " + port);
                String status = input.readLine().trim();
                System.out.println("Node" + address[0] + ":" + address[1] + " -=- " + status);
                if (status.equals("welcome")) { //Status "Welcome"
                    neighbours.add(new InetSocketAddress(address[0], Integer.parseInt(address[1])));
                }
                input.close();
                output.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println("Failed to connect to " + address[0] + ":" + address[1]);
            }
        }
        //Second, listen to new connections
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Problem opening server");
            System.exit(-1);
        }
        System.out.println("Node listens on port " + port);
        Socket client = null;
        while (true) {
            try {
                client = server.accept();
            } catch (IOException e) {
                System.out.println("Accept Exception");
                System.exit(-1);
            }
            (new NodeThread(client)).start();
        }
    }

    public class NodeThread extends Thread {
        private final Socket client;
        private TreeSet<Integer> opIds;

        public NodeThread(Socket client) {
            super();
            this.client = client;
            opIds = new TreeSet<>();
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter clientOutput = new PrintWriter(client.getOutputStream(), true);
                String[] tmp = input.readLine().trim().split(" ");
                String operation, argument = null, reply = "ERROR";
                Integer id = null;
                if (tmp.length == 3) {
                    operation = tmp[0];
                    argument = tmp[1];
                    id = Integer.parseInt(tmp[2]);
                } else if (tmp.length == 2) {
                    operation = tmp[0];
                    argument = tmp[1];
                } else
                    operation = tmp[0];
                if (opIds.contains(id))
                    clientOutput.println("loop");
                else {
                    if (id != null)
                        opIds.add(id);
                    switch (operation) {
                        case "set-value" -> {
                            tmp = argument.split(":");
                            if (Integer.parseInt(tmp[0]) == record.getKey()) {
                                record.setValue(Integer.parseInt(tmp[1]));
                                reply = "OK";
                            } else {
                                if (id == null)
                                    id = opIds.last() + 1;
                                for (InetSocketAddress neighbour : neighbours) {
                                    Socket temporary = new Socket();
                                    temporary.connect(neighbour);
                                    PrintWriter output = new PrintWriter(temporary.getOutputStream(), true);
                                    input = new BufferedReader(new InputStreamReader(temporary.getInputStream()));
                                    output.println(operation + " " + argument + " " + id);
                                    if (input.readLine().equals("OK " + id)) {
                                        reply = "OK";
                                        output.close();
                                        temporary.close();
                                        break;
                                    }
                                    output.close();
                                    temporary.close();
                                }
                            }
                        }
                        case "get-value" -> {
                            if (Integer.parseInt(argument) == record.getKey())
                                reply = record.toString();
                            else {
                                if (id == null)
                                    id = opIds.last() + 1;
                                for (InetSocketAddress neighbour : neighbours) {
                                    Socket temporary = new Socket();
                                    temporary.connect(neighbour);
                                    PrintWriter output = new PrintWriter(temporary.getOutputStream(), true);
                                    input = new BufferedReader(new InputStreamReader(temporary.getInputStream()));
                                    output.println(operation + " " + argument + " " + id);
                                    String answer = input.readLine().trim();
                                    if (answer.matches(argument + ":\\d+" + " " + id)) {
                                        reply = answer.split(" ")[0];
                                        output.close();
                                        temporary.close();
                                        break;
                                    }
                                    output.close();
                                    temporary.close();
                                }
                            }
                        }
                        case "find-key" -> {
                            if (Integer.parseInt(argument) == record.getKey())
                                reply = InetAddress.getLocalHost().getHostAddress() + ":" + client.getLocalPort();
                            else {
                                if (id == null)
                                    id = opIds.last();
                                for (InetSocketAddress neighbour : neighbours) {
                                    Socket temporary = new Socket();
                                    temporary.connect(neighbour);
                                    PrintWriter output = new PrintWriter(temporary.getOutputStream(), true);
                                    input = new BufferedReader(new InputStreamReader(temporary.getInputStream()));
                                    output.println(operation + " " + argument + " " + id);
                                    String answer = input.readLine().trim();
                                    if (answer.matches("*:\\d+" + " " + id)) {
                                        reply = answer.split(" ")[0];
                                        output.close();
                                        temporary.close();
                                        break;
                                    }
                                    output.close();
                                    temporary.close();
                                }
                            }
                        }
                        case "get-max" -> {
                            //ToDo get the biggest value
                        }
                        case "get-min" -> {
                            //ToDo get the smallest value
                        }
                        case "new-record" -> {
                            tmp = argument.split(":");
                            record = new DatabaseRecord(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
                            clientOutput.println("OK");
                        }
                        case "terminate" -> {
                            System.out.println("Termination");
                            for (InetSocketAddress neighbour : neighbours) {
                                Socket temporary = new Socket();
                                temporary.connect(neighbour);
                                PrintWriter output = new PrintWriter(temporary.getOutputStream(), true);
                                output.println("terminated");
                                output.close();
                                temporary.close();
                            }
                            clientOutput.println("OK");
                            input.close();
                            clientOutput.close();
                            client.close();
                            System.exit(0);
                        }
                        case "terminated" ->
                                neighbours.remove(new InetSocketAddress(client.getLocalAddress(), client.getPort()));
                        case "newnode" ->
                                neighbours.add(new InetSocketAddress(client.getLocalAddress(), Integer.parseInt(argument)));
                        default -> System.out.println("Wrong operation Input!");
                    }
                    if (neighbours.contains(new InetSocketAddress(client.getLocalAddress(), client.getPort())))
                        clientOutput.println(reply + " " + id);
                    else
                        clientOutput.println(reply);
                }
                input.close();
                clientOutput.close();
                client.close();
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
