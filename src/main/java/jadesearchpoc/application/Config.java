package jadesearchpoc.application;

public class Config {
	// placeholder class for configuration parameters
	// these parameters will likely be read from a file or fetched from a profile later

	public static final String ElasticSearchIPAddress = "35.232.178.35";
	public static final int ElasticSearchPort = 9200;

	public static final String DataRepoIPAddress = "https://jade-mm.datarepo-dev.broadinstitute.org";
	public static final String GoogleDataProjectId = "broad-jade-mm-data";
	public static final String DataRepoClientName = "jade-search-poc";

	public static final String ClientSecretFilePath = "/Users/marikomedlock/.jadecli/client/jadecli_client_secret.json";
	public static final String CredentialsDirectory = "/Users/marikomedlock/.jadecli/creds/";

	// ch.qos.logback.classic.Level : ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
	public static final String LoggerLevel = "DEBUG";

	private Config() { }
}