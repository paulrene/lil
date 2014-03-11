package no.leinstrandil;

import org.kohsuke.args4j.Option;

public class Config {

    @Option(name = "-appid", usage = "Sets the Facebook app id", required = true)
    private String appId;
    @Option(name = "-appsecret", usage = "Sets the Facebook app secret", required = true)
    private String appSecret;
    @Option(name = "-baseurl", usage = "Sets the webapp base url", required = false)
    private String baseUrl = "http://localhost:8080/";
    @Option(name = "-port", usage = "Sets the webapp base url", required = false)
    private int port = 8080;

    public Config() {
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getPort() {
        return port;
    }

}
