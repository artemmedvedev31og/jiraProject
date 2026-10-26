import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @brief Модель данных найденной композиции Genius API.
 *
 * Хранит имя исполнителя, название музыкального трека и URL страницы композиции.
 * Имена полей сопоставляются с соответствующими свойствами JSON-ответа.
 */
public class Result {
    /** Имя исполнителя или перечень имён в формате, возвращённом Genius API. */
    @JsonProperty("artist_names")
    private String artistNames;

    /** @return URL страницы композиции на Genius */
    public String getUrl() {
        return url;
    }

    /** @param url URL страницы композиции на Genius */
    public void setUrl(String url) {
        this.url = url;
    }

    /** Ссылка на страницу трека, JSON-поле {@code url}. */
    @JsonProperty("url")
    private String url;

    /** @return название композиции */
    public String getTitle() {
        return title;
    }

    /** @param title название композиции */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return имя исполнителя */
    public String getArtistNames() {
        return artistNames;
    }

    /** @param artistNames имя исполнителя */
    public void setArtistNames(String artistNames) {
        this.artistNames = artistNames;
    }

    /** Название композиции, JSON-поле {@code title}. */
    @JsonProperty("title")
    private String title;
}
