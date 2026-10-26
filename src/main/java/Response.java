import java.util.List;

/**
 * @brief Модель внутреннего объекта {@code response} Genius API.
 *
 * Содержит список элементов выдачи, полученных в результате поиска.
 */
public class Response {
    /** @return список элементов выдачи */
    public List<Hits> getHits() {
        return hits;
    }

    /** @param hits список элементов выдачи */
    public void setHits(List<Hits> hits) {
        this.hits = hits;
    }

    /** Элементы поисковой выдачи из JSON-поля {@code hits}. */
    private List<Hits> hits;
}
