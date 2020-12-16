package elements;

public enum RequestType {

    GET("get"), SET("set"), DELETE("delete"), EXIT("exit");

    private final String type;

    RequestType(String type) {
        this.type = type;
    }

    public static RequestType find(String type) {
        for (RequestType value : RequestType.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }

}
