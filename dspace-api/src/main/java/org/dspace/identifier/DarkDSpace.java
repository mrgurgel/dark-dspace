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
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.dspace.services.factory.DSpaceServicesFactory.getInstance;

public class DarkDSpace extends DOI {

    public static final String ALLOWED_METADATA = "dc.contributor.author,dc.title,dc.identifier.uri,dc.date.issued";
    public static final int DELTA_FOR_DARK_STATUS = 100;
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(DarkDSpace.class);

    private DOI persistentDark;

    private Dark darkComponent;

    /**
     * Encapsulates a DOI record as a Dark object
     *
     * @param doi
     */
    public DarkDSpace(DOI doi) {
        inicializeDarkComponent();
        this.persistentDark = doi;
    }


    /**
     * Create and persist a new DOI/Dark record
     * @param inProgressSubmission
     * @param context
     */
    public DarkDSpace(InProgressSubmission inProgressSubmission, Context context) {
        try {
            inicializeDarkComponent();
            DOI dark = saveDoiAsDark(inProgressSubmission, context);
            this.persistentDark = dark;
        } catch (Exception e) {
            LOGGER.error(e);
        }

    }

    private void inicializeDarkComponent() {
        String repoPrefix = getInstance().getConfigurationService().getProperty("darkpid.repo.prefix");
        String baseUrl = getInstance()
                .getConfigurationService().getProperty("darkpid.base.url");

        darkComponent = Dark.newInstance(baseUrl, repoPrefix);
    }

    private DOI saveDoiAsDark(InProgressSubmission inProgressSubmission, Context context) throws SQLException {
        String darkPid = darkComponent.createNewPid();

        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();

        ContentServiceFactory.getInstance().getItemService()
                .addMetadata(context, inProgressSubmission.getItem(), "dc", "identifier", "uri",
                        null, darkPid);

        DOI dark = doiService.create(context);
        dark.setDoi(darkPid);
        dark.setDSpaceObject(inProgressSubmission.getItem());
        dark.setStatus(DarkStatus.TO_BE_REGISTERED.value);

        return new DarkDSpace(dark);
    }


    public void registerData() {
        fullFillDarkBody(persistentDark);
        sendUri(persistentDark);
    }

    public void fullFillDarkBody(DOI persistentDark) {
        try {
            Item darkDSpaceItem = (Item) persistentDark.getDSpaceObject();

            if (darkDSpaceItem.isWithdrawn()) {

                List<MetadataValue> metadata = darkDSpaceItem.getMetadata();

                List<String> allowedMetadata = Arrays.asList(ALLOWED_METADATA.split(","));
                List<String> requestedMetadata = Arrays.asList(getInstance().getConfigurationService().getProperty("darkpid.send.metadata").split(","));

                DarkDataVO darkDataVO = new DarkDataVO();
                darkDataVO.withDarkId(persistentDark.getDoi());

                if (hasToAddMetadata(allowedMetadata, requestedMetadata, "dc.title")) {

                    extractMetadataValues(metadata, "dc", "title", null)
                            .ifPresent(metadataValue -> darkDataVO.withTitle(metadataValue.getValue()));
                }

                if (hasToAddMetadata(allowedMetadata, requestedMetadata, "dc.contributor.author")) {

                    extractMetadataValues(metadata, "dc", "contributor", "author")
                            .ifPresent(metadataValue -> darkDataVO.withAutor(metadataValue.getValue()));

                }

                if (hasToAddMetadata(allowedMetadata, requestedMetadata, "dc.date.issued")) {

                    extractMetadataValues(metadata, "dc", "date", "issued")
                            .ifPresent(metadataValue -> darkDataVO.withYear(metadataValue.getValue()));

                }

                if (hasToAddMetadata(allowedMetadata, requestedMetadata, "dc.identifier.uri")) {

                    extractMetadataValues(metadata, "dc", "uri", null)
                            .ifPresent(metadataValue -> darkDataVO.withUrl(metadataValue.getValue()));
                }

                darkComponent.sendPayload(darkDataVO, persistentDark.getDoi());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasToAddMetadata(List<String> allowedMetadata, List<String> requestedMetadata, String desiredMetadata) {
        return allowedMetadata.contains(desiredMetadata) && requestedMetadata.contains(desiredMetadata);
    }

    public void sendUri(DOI persistentDark) {
        try {
            Item darkDSpaceItem = (Item) persistentDark.getDSpaceObject();

            DarkDataVO darkDataVO = DarkDataVO.createNew();
            String externalUrl = getInstance().getConfigurationService().
                    getProperty("dspace.ui.url") + "/" + darkDSpaceItem.getHandle();
            darkDataVO.withExternalUrl(externalUrl);

            String repoPrefix = getInstance().getConfigurationService().getProperty("darkpid.repo.prefix");
            String baseUrl = getInstance()
                    .getConfigurationService().getProperty("darkpid.base.url");

            Dark.newInstance(repoPrefix, baseUrl).sendExternalUrl(externalUrl, persistentDark.getDoi());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public void setToBeDeleted() {
        setStatus(DarkStatus.TO_BE_DELETED.value);
    }

    @Override
    public void setUpdateRegistered() {
        setStatus(DarkStatus.UPDATE_REGISTERED.value);
    }

    @Override
    public void setUpdateBeforeRegistration() {
        setStatus(DarkStatus.UPDATE_BEFORE_REGISTRATION.value);
    }

    @Override
    public boolean isIsUpdateBeforeRegistration() {
        return getStatus().equals(DarkStatus.UPDATE_BEFORE_REGISTRATION);
    }

    @Override
    public void setUpdateReserved() {
        setStatus(DarkStatus.UPDATE_RESERVED.value);
    }

    @Override
    public boolean isUpdateReserved() {
        return getStatus().equals(DarkStatus.UPDATE_RESERVED);

    }

    @Override
    public boolean isUpdateRegistered() {
        return getStatus().equals(DarkStatus.UPDATE_REGISTERED);
    }

    @Override
    public void setIsRegistered() {
        setStatus(DarkStatus.IS_REGISTERED.value);
    }

    @Override
    public void setIsReserved() {
        setStatus(DarkStatus.IS_RESERVED.value);
    }

    @Override
    public void setDeleted() {
        setStatus(DarkStatus.DELETED.value);
    }

    @Override
    public void setToBeReserved() {
        setStatus(DarkStatus.TO_BE_RESERVED.value);
    }

    @Override
    public boolean isToBeReserved() {
        return getStatus().equals(DarkStatus.TO_BE_RESERVED);
    }

    @Override
    public boolean isToBeRegesitered() {
        return getStatus().equals(DarkStatus.TO_BE_REGISTERED.value);
    }

    @Override
    public void setMinted() {
        setStatus(DarkStatus.MINTED.value);
    }

    @Override
    public boolean isMinted() {
        return getStatus().equals(DarkStatus.PENDING.value);
    }

    @Override
    public void setPending() {
        setStatus(DarkStatus.PENDING.value);
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