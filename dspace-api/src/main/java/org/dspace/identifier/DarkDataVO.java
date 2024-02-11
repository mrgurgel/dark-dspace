package org.dspace.identifier;

import java.text.MessageFormat;

public class DarkDataVO {


    private String author;
    private String title;
    private String year;
    private String url;
    private String externalUrl;

    protected String darkId;

    public static DarkDataVO createNew() {
        return new DarkDataVO();
    }

    public DarkDataVO withAutor(String author) {
        this.author = author;
        return this;
    }

    public DarkDataVO withYear(String year) {
        this.year = year;
        return this;
    }

    public DarkDataVO withUrl(String url) {
        this.url = url;
        return this;
    }

    public DarkDataVO withTitle(String title) {
        this.title = title;
        return this;
    }

    public DarkDataVO withDarkId(String darkId) {
        this.darkId = darkId;
        return this;
    }


    public String bodyAsJson() {
        return MessageFormat.format( "{\n" +
                "  \"payload\": {\n" +
                "    \"author\": \"{0}\",\n" +
                "    \"title\": \"{1}\",\n" +
                "    \"year\": \"{2}\",\n" +
                "    \"url\": \"{3}\",\n" +
                "    \"darkId\": \"{4}\"\n" +
                "  }\n" +
                "}", author, title, year, url, darkId);
    }

    public String externalUrlAsJson() {
        return MessageFormat.format( "{\n" +
                "    \"external_url\": \"{0}\"\n" +
                "  }\n" +
                "}", externalUrl);
    }


    public DarkDataVO withExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
        return this;
    }
}