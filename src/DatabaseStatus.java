public class DatabaseStatus {
    public static String convert(String statusCode){
        switch (statusCode){
            case "10"-> {return "Welcome";}
            default -> {return "No such status code";}
        }
    }
}
