package gitlab.settings;


public enum SettingError {
    NOT_A_URL("Provided server url is not valid (must be in format http(s)://server.com)"),
    SERVER_CANNOT_BE_REACHED("Provided server cannot be reached"),
    INVALID_API_TOKEN("Provided API Token is not valid"),
    GENERAL_ERROR("Error during connection to server");

    final String message;

    SettingError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
