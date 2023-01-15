public class DatabaseClient {
    private String address;

    public static void main(String[] args) {
        if (args.length > 2) {
            DatabaseClient databaseClient = new DatabaseClient();
            if (args[0] == "-gateway")
                databaseClient.setAddress(args[1]);
            if (args[2] == "-operation")
                System.out.println(databaseClient.operate(args[3], args[4]));
        } else {
            System.out.println("Wrong input!");
            throw new IllegalArgumentException();
        }
    }

    public String operate(String operation, String argument) {
        //ToDo connect to gateway
        return null;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
