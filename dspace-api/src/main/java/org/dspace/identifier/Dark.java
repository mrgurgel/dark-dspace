package org.dspace.identifier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.dspace.services.factory.DSpaceServicesFactory.getInstance;

public class Dark extends DOI {

    public static final String PREFIX_HYPERDRIVE = "dark:/{1}";
    public static final String ALLOWED_METADATA = "dc.contributor.author,dc.title,dc.identifier.uri,dc.date.issued";
    public static final int DELTA_FOR_DARK_STATUS = 100;
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(Dark.class);

    private DOI persistentDark;
    private Context context;

    public Dark(DOI doi, Context context) {
        this.persistentDark = doi;
        this.context = context;
    }


    public static Dark createNewDarkPid(Context context, InProgressSubmission inProgressSubmission) {
        try {

            JsonObject jsonResponse = sendDarkPost("/core/new", "{}");
            String newPid = jsonResponse.get("ark").getAsString();

            String darkPid = MessageFormat.format(PREFIX_HYPERDRIVE, newPid);
            ContentServiceFactory.getInstance().getItemService()
                    .addMetadata(context, inProgressSubmission.getItem(), "dc", "identifier", "uri",
                    null, darkPid);

            DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
            Dark dark = new Dark(doiService.create(context),context);
            dark.setDoi(darkPid);
            dark.setDSpaceObject(inProgressSubmission.getItem());
            dark.setStatus(DarkStatus.TO_BE_REGISTERED.value);

            return dark;

        } catch (Exception e) {
            LOGGER.error(e);
        }

        return null;
    }

    public void registerData() {
        fullFillDarkBody(persistentDark);
        sendUri(persistentDark);
    }

    public void fullFillDarkBody(DOI persistentDark) {
        try {
            Item darkDSpaceItem = (Item) persistentDark.getDSpaceObject();

            List<MetadataValue> metadata = darkDSpaceItem.getMetadata();
            JsonObject darkBody = new JsonObject();

            List<String> allowedMetadata = Arrays.asList(ALLOWED_METADATA.split(","));

            if (allowedMetadata.contains("dc.title")) {
                extractMetadataValues(metadata, "dc", "title", null)
                        .ifPresent(metadataValue -> darkBody.addProperty("title", metadataValue.getValue()));

            }

            if (allowedMetadata.contains("dc.contributor.author")) {
                extractMetadataValues(metadata, "dc", "contributor", "author")
                        .ifPresent(metadataValue -> darkBody.addProperty("author", metadataValue.getValue()));

            }

            if (allowedMetadata.contains("dc.date.issued")) {
                extractMetadataValues(metadata, "dc", "date", "issued")
                        .ifPresent(metadataValue -> darkBody.addProperty("year", metadataValue.getValue()));

            }

            extractMetadataValues(metadata, "dc", "uri", null)
                    .ifPresent(metadataValue -> darkBody.addProperty("url", metadataValue.getValue()));

            JsonObject darkPostContainer = new JsonObject();
            darkPostContainer.addProperty("payload", darkBody.getAsString());

            String uri = MessageFormat.format(
                    "/core/set/{0}/{1}",
                    getInstance().getConfigurationService().getProperty("darkpid.repo.prefix"),
                    getDarkPidMatcher(persistentDark.getDoi()).group(2)
            );
            sendDarkPost(uri, darkPostContainer.getAsString());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendUri(DOI persistentDark) {
        try {
            Item darkDSpaceItem = (Item) persistentDark.getDSpaceObject();
            JsonObject darkBody = new JsonObject();

            darkBody.addProperty("external_url",
                    getInstance().getConfigurationService().
                            getProperty("dspace.ui.url") + "/" + darkDSpaceItem.getHandle());

            String uri = MessageFormat.format(
                    "/core/set/{0}/{1}",
                    getInstance().getConfigurationService().getProperty("darkpid.repo.prefix"),
                    getDarkPidMatcher(persistentDark.getDoi()).group(2)
            );

            sendDarkPost(uri, darkBody.getAsString());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static Matcher getDarkPidMatcher(String dark) {
        Pattern compile = Pattern.compile("dark\\:\\/(.*)\\/(.*)");
        Matcher matcher = compile.matcher(dark);
        matcher.find();
        return matcher;
    }

    private static Optional<MetadataValue> extractMetadataValues(List<MetadataValue> metadata, String schema, String element, Object qualifier) {
        return metadata.stream().filter(metadataValue ->
                metadataValue.getMetadataField().getMetadataSchema().equals(schema) &&
                        metadataValue.getMetadataField().getElement().equals(element) &&
                        metadataValue.getMetadataField().getQualifier().equals(qualifier)).findFirst();
    }

    private static JsonObject sendDarkPost(String uri, String body) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(getInstance()
                        .getConfigurationService().getProperty("darkpid.base.url") + uri))
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .headers("Content-Type", "application/json")
                .build();

        LOGGER.info("Sending Dark Request: " + request.toString());

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject jsonResponse = new JsonParser().parse(response.body()).getAsJsonObject();
        LOGGER.info("DARK response: " + jsonResponse);

        return jsonResponse;
    }

    @Override
    public boolean isRegistered() {
        return DarkStatus.IS_REGISTERED.value.equals(persistentDark.getStatus());
    }

    @Override
    public boolean isDeleted() {
        return DarkStatus.DELETED.value.equals(persistentDark.getStatus());
    }

    @Override
    public boolean isToBeDeleted() {
        return DarkStatus.TO_BE_DELETED.value.equals(persistentDark.getStatus());
    }

    @Override
    public void setToBeRegistered() {
        setStatus(DarkStatus.TO_BE_REGISTERED.value);
    }

    @Override
    public Integer getID() {
        return persistentDark.getID();
    }

    @Override
    public String getDoi() {
        return persistentDark.getDoi();
    }

    @Override
    public void setDoi(String doi) {
        persistentDark.setDoi(doi);
    }

    @Override
    public DSpaceObject getDSpaceObject() {
        return persistentDark.getDSpaceObject();
    }

    @Override
    public void setDSpaceObject(DSpaceObject dSpaceObject) {
        persistentDark.setDSpaceObject(dSpaceObject);
    }

    @Override
    public Integer getResourceTypeId() {
        return persistentDark.getResourceTypeId();
    }

    @Override
    public Integer getStatus() {
        return persistentDark.getStatus() + DELTA_FOR_DARK_STATUS;
    }

    public void setRegistered() {
        setStatus(DarkStatus.IS_REGISTERED.value);
    }

    @Override
    public void setStatus(Integer status) {
        persistentDark.setStatus(status);
    }

    public enum DarkStatus {
        TO_BE_REGISTERED(101),
        TO_BE_RESERVED(102),
        IS_REGISTERED(103),
        IS_RESERVED(104),
        UPDATE_RESERVED(105),
        UPDATE_REGISTERED(106),
        UPDATE_BEFORE_REGISTRATION(107),
        TO_BE_DELETED(108),
        DELETED(109),
        PENDING(110),
        MINTED(111);

        public Integer value;

        private DarkStatus(Integer value) {
            this.value = value;
        }
    }
}