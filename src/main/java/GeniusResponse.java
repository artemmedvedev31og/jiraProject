import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @brief Корневая модель JSON-ответа Genius API.
 *
 * Соответствует верхнему уровню JSON-объекта, в котором данные поиска находятся
 * в свойстве {@code response}.
 */
public class GeniusResponse {

    /** @return объект с результатами поиска */
    public Response getResponse() {
        return response;
    }

    /** @param response объект с результатами поиска */
    public void setResponse(Response response) {
        this.response = response;
    }

    /** Данные поискового ответа, соответствующие JSON-полю {@code response}. */
    @JsonProperty("response")
    private Response response;
}
