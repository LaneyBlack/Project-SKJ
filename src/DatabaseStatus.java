public class DatabaseStatus {
    public static String convert(String statusCode){
        return switch (statusCode){
            case "10"-> "Welcome";
            case "20"->"OK";
            case "30"->"Record Not Found";
            case "99"->"Error";
            default -> "No such status code";
        };
    }
}
