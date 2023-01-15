import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

    //ToDo what if record key already exists
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
                }
            databaseNode.action(toConnect);
        } else {
            System.out.println("Wrong input!");
            throw new IllegalArgumentException();
        }
    }

    public void action(Queue<String> toConnect) {
        //First, connect to queued Nodes
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(), port);
            PrintWriter output;
            BufferedReader input;
            while (!toConnect.isEmpty()) {
                String[] address = toConnect.poll().split(":");
                socket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 500);
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output.println("-newnode " + record);
                String status = input.readLine().trim();
                System.out.println("Node" + address[0] + ":" + address[1] + " -=- " + status + " " + DatabaseStatus.convert(status));
                if (status.equals("10")) { //Status "Welcome"
//                    output.println("-get-record");
//                    String[] record = input.readLine().trim().split(":");
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
        Socket connected = null;
        while (true) {
            try {
                connected = socket.accept();
            } catch (IOException e) {
                System.out.println("Accept Exception");
                System.exit(-1);
            }
            (new NodeThread(connected)).start();
        }
    }

    public class NodeThread extends Thread {
        private final Socket connected;

        public NodeThread(Socket connected) {
            super();
            this.connected = connected;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(connected.getInputStream()));
                PrintWriter output = new PrintWriter(connected.getOutputStream(), true);
                String operation = input.readLine().trim();
                //ToDo finish operations and check is it operation
                switch (operation) {
                    case "-set-value" -> {

                    }
                    case "-get-value" -> {

                    }
                    case "-find-value" -> {

                    }
                    case "-get-max" -> {

                    }
                    case "-get-min" -> {

                    }
                    case "-new-record" -> {

                    }
                    case "-terminate" -> {
                        for (InetSocketAddress neighbour : neighbours)
                            output.println("-terminated");
                    }
                    case "-terminated" -> {
                        neighbours.remove(connected);
                    }
                    case "-newnode" -> {
                        neighbours.add(new InetSocketAddress(connected.getLocalAddress(), connected.getPort()));
                    }
                }


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
