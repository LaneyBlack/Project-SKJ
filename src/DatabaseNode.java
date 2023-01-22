import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

public class DatabaseNode {
    private ServerSocket server;
    private final ArrayList<InetSocketAddress> neighbours;
    private DatabaseRecord record;
    private int port;
    private TreeSet<UUID> opIds;

    public DatabaseNode() {
        neighbours = new ArrayList<>();
        opIds = new TreeSet<>();
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
        Socket socket;
        while (!toConnect.isEmpty()) {
            String[] address = toConnect.poll().split(":");
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 1500);
                socket.setSoTimeout(1500);
                PrintWriter output;
                BufferedReader input;
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output.println("newnode " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
                String status = input.readLine().toLowerCase().trim();
                System.out.println("Node " + address[0] + ":" + address[1] + " -=- " + status);
                if (status.equals("welcome")) { //Status "Welcome"
                    neighbours.add(new InetSocketAddress(address[0], Integer.parseInt(address[1])));
                }
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
        System.out.println("Node listens on port " + port + ", while containing " + record);
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

        public NodeThread(Socket client) {
            super();
            this.client = client;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter clientOutput = new PrintWriter(client.getOutputStream(), true);
                String command = input.readLine().trim();
                System.out.print(command);
                System.out.println(" from " + client.getPort());
                String[] tmp = command.split(" ");
                String operation = null, argument = null, reply = "ERROR";
                UUID id = null;
                switch (tmp.length) {
                    case 3:
                        id = UUID.fromString(tmp[2]);
                    case 2:
                        argument = tmp[1];
                    default:
                        operation = tmp[0];
                        break;
                }
                if (id != null && opIds.contains(id) && !operation.equals("done"))
                    clientOutput.println("loop " + id);
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
                                if (id == null) {
                                    id = UUID.randomUUID();
                                    opIds.add(id);
                                }
                                if (!neighbours.isEmpty())
                                    for (InetSocketAddress neighbour : neighbours) {
                                        String answer = sendOpAndGetReply(neighbour, operation + " " + argument + " " + id);
                                        if (answer.equals("OK " + id)) {
                                            reply = "OK";
                                            break;
                                        }
                                    }
                            }
                        }
                        case "get-value" -> {
                            if (Integer.parseInt(argument) == record.getKey())
                                reply = record.toString();
                            else {
                                if (id == null) {
                                    id = UUID.randomUUID();
                                    opIds.add(id);
                                }
                                if (!neighbours.isEmpty())
                                    for (InetSocketAddress neighbour : neighbours) {
                                        String answer = sendOpAndGetReply(neighbour, operation + " " + argument + " " + id);
                                        if (answer.matches(argument + ":\\d+" + " " + id)) {
                                            reply = answer.split(" ")[0];
                                            break;
                                        }
                                    }
                            }
                        }
                        case "find-key" -> {
                            if (Integer.parseInt(argument) == record.getKey())
                                reply = InetAddress.getLocalHost().getHostAddress() + ":" + client.getLocalPort();
                            else {
                                if (id == null) {
                                    id = UUID.randomUUID();
                                    opIds.add(id);
                                }
                                if (!neighbours.isEmpty())
                                    for (InetSocketAddress neighbour : neighbours) {
                                        String answer = sendOpAndGetReply(neighbour, operation + " " + argument + " " + id);
                                        if (answer.matches(".*:\\d+" + " " + id)) {
                                            reply = answer.split(" ")[0];
                                            break;
                                        }
                                    }
                            }
                        }
                        case "get-max" -> {
                            client.setSoTimeout(1500);
                            DatabaseRecord max = record;
                            if (id == null) {
                                id = UUID.randomUUID();
                                opIds.add(id);
                            }
                            if (!neighbours.isEmpty())
                                for (InetSocketAddress neighbour : neighbours) {
                                    String answer = sendOpAndGetReply(neighbour, operation + " - " + id);
                                    if (!answer.split(" ")[0].equals("loop"))
                                        if (max.getValue() < Integer.parseInt(answer.split(" ")[0].split(":")[1])) {
                                            tmp = answer.split(" ")[0].split(":");
                                            max = new DatabaseRecord(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
                                        }
                                }
                            reply = max.toString();
                        }
                        case "get-min" -> {
                            client.setSoTimeout(1500);
                            DatabaseRecord min = record;
                            if (id == null) {
                                id = UUID.randomUUID();
                                opIds.add(id);
                            }
                            if (!neighbours.isEmpty())
                                for (InetSocketAddress neighbour : neighbours) {
                                    String answer = sendOpAndGetReply(neighbour, operation + " - " + id);
                                    if (!answer.split(" ")[0].equals("loop"))
                                        if (min.getValue() > Integer.parseInt(answer.split(" ")[0].split(":")[1])) {
                                            tmp = answer.split(" ")[0].split(":");
                                            min = new DatabaseRecord(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
                                        }
                                }
                            reply = min.toString();
                        }
                        case "new-record" -> {
                            tmp = argument.split(":");
                            record = new DatabaseRecord(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]));
                            reply = "OK";
                        }
                        case "terminate" -> {
                            if (!neighbours.isEmpty())
                                for (InetSocketAddress neighbour : neighbours) {
                                    Socket temporary = new Socket();
                                    temporary.setSoTimeout(1500);
                                    temporary.connect(neighbour);
                                    PrintWriter output = new PrintWriter(temporary.getOutputStream(), true);
                                    output.println("terminated " + port);
                                    output.close();
                                    temporary.close();
                                }
                            clientOutput.println("OK");
                            input.close();
                            clientOutput.close();
                            client.close();
                            System.exit(0);
                        }
                        case "terminated" -> {
                            reply = "Terminated " + client.getPort();
                            neighboursRemove(Integer.parseInt(argument));
                        }
                        case "newnode" -> {
                            tmp = argument.trim().split(":");
                            neighbours.add(new InetSocketAddress(tmp[0], Integer.parseInt(tmp[1])));
                            reply = "welcome";
                        }
                        default -> System.out.println("Wrong operation Input!");
                    }
                    if (id != null)
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

    public String sendOpAndGetReply(InetSocketAddress neighbour, String send) {
        try {
            Socket temporary = new Socket();
            temporary.setSoTimeout(1500);
            temporary.connect(neighbour);
            PrintWriter output = new PrintWriter(temporary.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(temporary.getInputStream()));
            output.println(send);
            String result = input.readLine().trim();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void neighboursRemove(int port) {
        for (InetSocketAddress neighbour : neighbours)
            if (neighbour.getPort() == port) {
                neighbours.remove(neighbour);
                break;
            }
    }


    public void setPort(int port) {
        this.port = port;
    }

    public void setRecord(String key, String value) {
        record = new DatabaseRecord(Integer.parseInt(key), Integer.parseInt(value));
    }
}