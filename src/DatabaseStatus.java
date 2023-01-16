public class DatabaseStatus {
    public static String convert(String statusCode){
        switch (statusCode){
            case "10"-> {return "Welcome";}
            case "20"->{return "OK";}
            case "30"->{return "Record Not Found";}
            case "99"->{return "Error";}
            default -> {return "No such status code";}
        }
    }
}
