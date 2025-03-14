package dev.buildcli.core.actions.dependency;

import com.google.gson.*;
import dev.buildcli.core.model.Dependency;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

public class DependencySearchService {

    Dependency dependency;

    private static final String API_MAVEN = "https://search.maven.org/solrsearch/select?q=";
    private static final String ROWS = "&rows=5";
    private static final String OUTPUT ="&wt=json";


    public HttpRequest createSearchGetRequest(String groupOrArtifactID){
        return HttpRequest.newBuilder()
                .uri(URI.create(API_MAVEN + groupOrArtifactID + ROWS + OUTPUT))
                .GET()
                .build();
    }

    private String getResponseBody(HttpRequest request){
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            client.close();
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> sendSearchRequest(String dependencyName) {
        dependencyName = URLEncoder.encode(dependencyName, StandardCharsets.UTF_8);

        HttpRequest request = createSearchGetRequest(dependencyName);

        String response = getResponseBody(request);

       return getDependencyList(response);
    }

    private List<String> getDependencyList(String response){
        JsonArray jsonArray  = new Gson().fromJson(response, JsonObject.class)
                .getAsJsonObject("response")
                .getAsJsonArray("docs");

        return StreamSupport.stream(jsonArray.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(this::getDependencyFromJson)
                .toList();
    }

    private String getDependencyFromJson(JsonObject jsonObject){
        return String.join("",jsonObject.get("g").getAsString(),
                ":",jsonObject.get("a").getAsString(),
                ":",jsonObject.get("latestVersion").getAsString());
    }
}