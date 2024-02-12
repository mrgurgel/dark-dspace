package org.dspace.identifier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Dark {

    public static final String PREFIX_HYPERDRIVE = "dark:/{0}";
    private static String _projectPrefix;
    private static String _baseUrl;


    private Dark() {

    }

    public static Dark newInstance(String baseUrl, String projectPrefix) {
        _baseUrl = baseUrl;
        _projectPrefix = projectPrefix;
        return new Dark();
    }

    public static Dark getInstance() {
        if(_baseUrl != null) {
            throw new RuntimeException("No data was initialized, call first #newInstance");
        }
        return new Dark();
    }

    public String createNewPid() {
        JsonObject jsonResponse = sendDarkPost(_baseUrl + "/core/new", "{}");
        assert jsonResponse != null;
        return MessageFormat.format(PREFIX_HYPERDRIVE, jsonResponse.get("ark").getAsString());
    }

    private static JsonObject sendDarkPost(String url, String body) {
        try {
            System.out.println("Data to be sent: " + body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .headers("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendPayload(DarkDataVO darkDataVO, String darkId) {
        String uri = MessageFormat.format("/core/set/{0}/{1}",
                _projectPrefix, getDarkPidMatcher(darkId).group(2));
        sendDarkPost(_baseUrl + "/" + uri, darkDataVO.bodyAsJson());
    }

    private static Matcher getDarkPidMatcher(String dark) {
        Pattern compile = Pattern.compile("dark\\:\\/(.*)\\/(.*)");
        Matcher matcher = compile.matcher(dark);
        matcher.find();
        return matcher;
    }

    public void sendExternalUrl(String externalUrl, String darkId) {

        String postData = DarkDataVO.createNew().withExternalUrl(externalUrl).externalUrlAsJson();

        String uri = MessageFormat.format("/core/set/{0}/{1}",
                _projectPrefix, getDarkPidMatcher(darkId).group(2));
        sendDarkPost(_baseUrl + "/" + uri, postData);

    }
}
