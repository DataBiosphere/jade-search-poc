package jadesearchpoc.application;

public class Config {
	// placeholder class for configuration parameters
	// e.g. ElasticSearch IP address and port, name of custom class to build the index document
	// these parameters will likely be read from a file or fetched from a profile later

	public static final String ElasticSearchIPAddress = "35.232.178.35";
	public static final String ElasticSearchPort = "9200";

	public static final String DataRepoIPAddress = "https://jade-mm.datarepo-dev.broadinstitute.org";
	public static final String GoogleProjectId = "broad-jade-mm";
	public static final String DataRepoClientName = "jade-search-poc";

	public static final String ClientSecretFilePath = "/Users/marikomedlock/.jadecli/client/jadecli_client_secret.json";
	public static final String CredentialsDirectory = "/Users/marikomedlock/.jadecli/creds/";

	private Config() { }
}