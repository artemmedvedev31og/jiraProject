import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * @brief HTTP-клиент для поиска музыкальных композиций через Genius API.
 *
 * Класс отвечает за формирование запросов к endpoint поиска Genius, передачу
 * токена авторизации, получение HTTP-ответа и преобразование JSON в модели
 * {@link GeniusResponse}, {@link Response}, {@link Hits} и {@link Result}.
 *
 * Реализация ограничивает количество обрабатываемых элементов первыми пятью
 * результатами ответа API. Тайм-аут подключения задаётся равным 12 секундам;
 * он ограничивает установление соединения, а не полное время выполнения запроса.
 *
 * Задачи Jira: SCRUM-9 (HTTP-клиент), SCRUM-10 (обработка JSON),
 * SCRUM-15 (сетевые ошибки и ожидание), SCRUM-16 (ограничение результатов).
 */
public class GeniusClient {
    private final HttpClient client;
    private String geniusApiUrl = "https://api.genius.com/search";
    private final ParserKeys parserKeys = new ParserKeys();
    private final Keys keys = parserKeys.getKeys();
    private String geniusApiToken = keys.getGeniusApiToken();
    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * @brief Создаёт клиент с endpoint и токеном из конфигурации.
     * @throws IOException если конфигурация с ключами не может быть прочитана.
     */
    public GeniusClient() throws IOException {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    /**
     * @brief Создаёт клиент с указанным базовым URL.
     *
     * Такой конструктор можно использовать, например, для тестирования с локальным
     * HTTP-сервером вместо реального Genius API.
     *
     * @param baseUrl базовый URL endpoint поиска
     * @throws IOException если конфигурация с ключами не может быть прочитана
     */
    public GeniusClient(String baseUrl) throws IOException {
        this.geniusApiUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    /**
     * @brief Создаёт клиент с явно переданными зависимостями.
     *
     * Конструктор удобен для тестирования: HTTP-клиент, преобразователь JSON и
     * объект с ключами можно подменить контролируемыми тестовыми экземплярами.
     *
     * @param client HTTP-клиент для отправки запросов
     * @param mapper преобразователь JSON в Java-объекты
     * @param keys конфигурация, содержащая токен Genius API
     * @throws IOException объявлено в контракте конструктора
     */
    public GeniusClient(HttpClient client, ObjectMapper mapper, Keys keys) throws IOException {
        this.client = client;
        this.mapper = mapper;
        this.geniusApiToken = keys.getGeniusApiToken();
        this.geniusApiUrl = "https://api.genius.com/search";
    }

    /**
     * @brief Ищет композиции и группирует их по имени исполнителя.
     *
     * Для запроса используется параметр {@code q}. Текст кодируется в UTF-8,
     * запрос отправляется методом GET с заголовком авторизации Bearer. Из ответа
     * разбираются до пяти первых элементов {@code hits}; названия группируются
     * в отображении «имя исполнителя → список названий треков».
     *
     * @param message поисковая строка, обычно имя исполнителя
     * @return отображение исполнителей и найденных треков; при HTTP-статусе,
     *         отличном от 200, текущая реализация возвращает {@code null}
     * @throws IOException при ошибке отправки запроса или разбора JSON
     * @throws InterruptedException если поток прерван во время отправки запроса
     */
    public Map<String, List<String>> searchArtists(String message) throws IOException, InterruptedException {
        String searchMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geniusApiUrl + "?q=" + searchMessage))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + geniusApiToken)
                .GET()
                .build();
        HttpResponse<String> responseServer = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (responseServer.statusCode() != 200) {
            System.err.println("Ошибка: " + responseServer.body());
            return null;
        }
        GeniusResponse geniusResponse = mapper.readValue(responseServer.body(), GeniusResponse.class);
        Response response = geniusResponse.getResponse();
        List<Hits> hits = response.getHits();
        Map<String, List<String>> songs = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(geniusResponse.getResponse().getHits().size(), 5); i++) {
            if (songs.containsKey(hits.get(i).getResult().getArtistNames())) {
                songs.get(hits.get(i).getResult().getArtistNames()).add(hits.get(i).getResult().getTitle());
                System.out.println(hits.get(i).getResult().getUrl());
            } else {
                songs.put(hits.get(i).getResult().getArtistNames(), new ArrayList<>());
                songs.get(hits.get(i).getResult().getArtistNames()).add(hits.get(i).getResult().getTitle());
            }
        }
        return songs;
    }

    /**
     * @brief Получает ссылки на первые пять найденных треков.
     *
     * Метод выполняет запрос к Genius API и извлекает URL из поля {@code result.url}
     * каждого элемента {@code hits}. Порядок ссылок соответствует порядку в ответе API.
     *
     * @param message поисковая строка
     * @return список URL первых пяти результатов; при HTTP-статусе, отличном от 200,
     *         текущая реализация возвращает {@code null}
     * @throws IOException при сетевой ошибке или ошибке разбора JSON
     * @throws InterruptedException если поток прерван во время отправки запроса
     */
    public List<String> getUrls(String message) throws IOException, InterruptedException {
        String searchMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geniusApiUrl + "?q=" + searchMessage))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + geniusApiToken)
                .GET()
                .build();
        HttpResponse<String> responseServer = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (responseServer.statusCode() != 200) {
            System.err.println("Ошибка: " + responseServer.body());
            return null;
        }
        GeniusResponse geniusResponse = mapper.readValue(responseServer.body(), GeniusResponse.class);
        Response response = geniusResponse.getResponse();
        List<Hits> hits = response.getHits();
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < Math.min(geniusResponse.getResponse().getHits().size(), 5); i++) {
            urls.add(hits.get(i).getResult().getUrl());
        }
        return urls;
    }
}
