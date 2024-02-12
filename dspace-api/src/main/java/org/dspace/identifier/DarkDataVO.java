package org.dspace.identifier;

import com.google.gson.Gson;

import java.util.Map;

public class DarkDataVO {


    private String author = "";
    private String title = "";
    private String year = "";
    private String url = "";
    private String externalUrl = "";

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
        return new Gson().toJson(
            Map.of("payload",   Map.of(
                        "author", author,
                        "title", title,
                        "year", year,
                        "url", url,
                        "darkId", darkId
                    ))
        ).toString();
    }

    public String externalUrlAsJson() {
        return new Gson().toJson(Map.of("external_url", externalUrl)).toString();
    }

    public DarkDataVO withExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
        return this;
    }
}
