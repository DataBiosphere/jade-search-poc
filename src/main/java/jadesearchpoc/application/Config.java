package jadesearchpoc.application;

public final class Config {
    // placeholder class for configuration parameters
    // these parameters will likely be read from a file or fetched from a profile later

    private static final String ElasticSearchIPAddress = "35.232.178.35";
    private static final int ElasticSearchPort = 9200;

    private static final String DataRepoIPAddress = "https://jade-mm.datarepo-dev.broadinstitute.org";
    private static final String GoogleDataProjectId = "broad-jade-mm-data";
    private static final String DataRepoClientName = "jade-search-poc";

    private static final String ClientSecretFilePath =
            "/Users/marikomedlock/.jadecli/client/jadecli_client_secret.json";
    private static final String CredentialsDirectory = "/Users/marikomedlock/.jadecli/creds/";

    // ch.qos.logback.classic.Level : ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
    private static final String LoggerLevel = "DEBUG";

    private Config() { }

    public static String getElasticSearchIPAddress() {
        return ElasticSearchIPAddress;
    }

    public static int getElasticSearchPort() {
        return ElasticSearchPort;
    }

    public static String getDataRepoIPAddress() {
        return DataRepoIPAddress;
    }

    public static String getGoogleDataProjectId() {
        return GoogleDataProjectId;
    }

    public static String getDataRepoClientName() {
        return DataRepoClientName;
    }

    public static String getClientSecretFilePath() {
        return ClientSecretFilePath;
    }

    public static String getCredentialsDirectory() {
        return CredentialsDirectory;
    }

    public static String getLoggerLevel() {
        return LoggerLevel;
    }
}
