import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @brief Обрабатывает пользовательские сообщения и сценарии меню бота.
 *
 * Класс направляет пользователя между главным меню, поиском по исполнителю,
 * выбором трека и действиями с выбранной композицией. Поисковый сценарий
 * использует {@link GeniusClient} для получения списка треков и ссылок.
 *
 * Задача Jira SCRUM-8: ввод имени исполнителя и запуск поиска.
 */
public class MainMenu {
    private final GeniusClient geniusClient;
    private final Buttons buttons = new Buttons();
    private final Map<Long, List<String>> userTrackList = new HashMap<>();
    private final Map<Long, List<String>> trackList = new HashMap<>();
    private int index = 0;

    /**
     * @brief Создаёт меню с клиентом Genius API по умолчанию.
     * @throws RuntimeException если не удалось создать {@link GeniusClient}
     */
    public MainMenu() {
        try {
            this.geniusClient = new GeniusClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @brief Создаёт меню с переданным клиентом Genius API.
     * @param geniusClient клиент для выполнения поисковых запросов
     */
    public MainMenu(GeniusClient geniusClient) {
        this.geniusClient = geniusClient;
    }

    /**
     * @brief Выбирает обработчик входящего сообщения по состоянию пользователя.
     *
     * Для нового пользователя регистрирует чат и возвращает приветственное сообщение.
     * Для зарегистрированного пользователя вызывает соответствующий обработчик меню,
     * поиска или действий с выбранным треком.
     *
     * @param update входящее обновление Telegram
     * @param userData хранилище пользовательских данных и состояний
     * @return сообщение, которое нужно отправить пользователю
     * @throws Exception если обработка сообщения завершается ошибкой
     */
    public SendMessage mainMenu(Update update, UserData userData) throws Exception {
        String message = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        SendMessage outMessage = new SendMessage();

        outMessage.setChatId(String.valueOf(chatId));
        outMessage.setParseMode(ParseMode.HTML);
        if (!userData.checkUser(chatId)) {
            userData.addUser(chatId);
            outMessage.setText(Messages.FIRST_MESSAGE);
            outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
            return outMessage;
        }
        UserState currentState = userData.checkUserState(chatId);
        switch (currentState) {
            case WAITING_FOR_ACTIONS:
                return handleMainMenu(message, userData, chatId);
            case WAITING_FOR_ARTISTS:
                return handleSearchMenu(message, userData, chatId);
            case WAITING_FOR_TRACK_SELECTION:
                return handleTrackSelection(message, userData, chatId);
            case WAITING_FOR_TEXT:
                return handleSendText(userData, chatId);
            case WAITING_FOR_LINK:
                return handleSendTrack(userData, chatId);
            default:
                outMessage.setText(Messages.DEFAULT_MESSAGE);
                outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
                return outMessage;
        }
    }

    /**
     * @brief Обрабатывает команды главного меню.
     * @param command введённая команда или текст кнопки
     * @param userData пользовательские данные и состояния
     * @param chatId идентификатор Telegram-чата
     * @return сообщение с результатом обработки команды
     */
    public SendMessage handleMainMenu(String command, UserData userData, long chatId) {
        SendMessage outMessage = new SendMessage();
        outMessage.setChatId(String.valueOf(chatId));
        outMessage.setParseMode(ParseMode.HTML);
        switch (command.toLowerCase()) {
            case "/help", "помощь":
                outMessage.setText(Messages.HELP_MESSAGE);
                break;
            case "/search", "поиск":
                userData.changeUserState(chatId, UserState.WAITING_FOR_ARTISTS);
                outMessage.setText(Messages.SEARCH_MESSAGE);
                break;
            default:
                outMessage.setText(Messages.DEFAULT_MESSAGE);
                break;
        }
        outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
        return outMessage;
    }

    /**
     * @brief Обрабатывает ввод имени исполнителя и запускает поиск треков.
     *
     * Команда «Назад» возвращает пользователя в главное меню. В остальных случаях
     * поисковая строка передаётся в {@link GeniusClient#searchArtists(String)} и
     * {@link GeniusClient#getUrls(String)}. Найденные данные сохраняются для последующих
     * действий с композицией, а клавиатура формируется из результатов поиска.
     *
     * @param search введённое имя исполнителя или поисковая строка
     * @param userData пользовательские данные и состояния
     * @param chatId идентификатор Telegram-чата
     * @return сообщение с результатами поиска, подсказкой или сообщением об ошибке
     */
    public SendMessage handleSearchMenu(String search, UserData userData, long chatId) {
        SendMessage outMessage = new SendMessage();
        outMessage.setChatId(String.valueOf(chatId));
        outMessage.setParseMode(ParseMode.HTML);
        if (search.equalsIgnoreCase("/back") || search.equalsIgnoreCase("назад")) {
            userData.changeUserState(chatId, UserState.WAITING_FOR_ACTIONS);
            outMessage.setText(Messages.BACK_MESSAGE);
            outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
            return outMessage;
        }
        try {
            Map<String, List<String>> songs = geniusClient.searchArtists(search);
            List<String> urls = geniusClient.getUrls(search);
            userTrackList.put(chatId, urls);
            List<String> tracks = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : songs.entrySet()) {
                tracks.add(entry.getKey() + "-" + songs.get(entry.getKey()).get(0));
            }
            trackList.put(chatId, tracks);

            if (songs == null || songs.isEmpty()) {
                outMessage.setText("По вашему запросу ничего не найдено.\nПопробуйте изменить запрос.");
                outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
                return outMessage;
            }
            outMessage.setText("Найденные треки:\n");
            List<List<InlineKeyboardButton>> buttonsRows = buttons.formatResult(songs);
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            keyboardMarkup.setKeyboard(buttonsRows);
            outMessage.setReplyMarkup(keyboardMarkup);

        } catch (Exception e) {
            outMessage.setText("Произошла ошибка при поиске.\nПопробуйте еще раз.");
            outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
            e.printStackTrace();
        }

        return outMessage;
    }

    /**
     * @brief Обрабатывает выбор найденного трека.
     * @param message текст или callback-значение, обозначающее выбранное действие
     * @param userData пользовательские данные и состояния
     * @param chatId идентификатор Telegram-чата
     * @return сообщение следующего шага взаимодействия
     * @throws IOException при ошибке работы с данными или внешним сервисом
     * @throws InterruptedException если прерван поток запроса
     */
    public SendMessage handleTrackSelection(String message, UserData userData, long chatId) throws IOException, InterruptedException {
        SendMessage outMessage = new SendMessage();
        outMessage.setChatId(String.valueOf(chatId));
        outMessage.setParseMode(ParseMode.HTML);
        if (message.equalsIgnoreCase("/back") || message.equalsIgnoreCase("назад")) {
            userData.changeUserState(chatId, UserState.WAITING_FOR_ACTIONS);
            outMessage.setText(Messages.BACK_MESSAGE);
            outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
            return outMessage;
        }
        if(message.startsWith("track_")){
            outMessage.setText("Что вы хотите выбрать?");
            index = Character.getNumericValue(message.charAt(6));
            userData.changeUserState(chatId, UserState.WAITING_FOR_TRACK_ACTIONS);
            outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
            return outMessage;
        }
        return outMessage;
    }

    /**
     * @brief Получает и отправляет текст песни, связанный с выбранным треком.
     * @param userData пользовательские данные, в том числе список URL треков
     * @param chatId идентификатор Telegram-чата
     * @return сообщение с текстом песни
     * @throws IOException если текст песни не удалось получить
     */
    public SendMessage handleSendText(UserData userData, long chatId) throws IOException {
        SendMessage outMessage = new SendMessage();
        outMessage.setChatId(String.valueOf(chatId));
        outMessage.setParseMode(ParseMode.HTML);
        GeniusParser geniusParser = new GeniusParser();
        userData.changeUserState(chatId, UserState.WAITING_FOR_ARTISTS);
        String lyrics = geniusParser.getCleanLyrics(userTrackList.get(chatId).get(index));
        outMessage.setText(lyrics);
        outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
        return outMessage;
    }

    /**
     * @brief Получает и отправляет ссылку на музыкальный сервис для выбранного трека.
     * @param userData пользовательские данные и список выбранных композиций
     * @param chatId идентификатор Telegram-чата
     * @return сообщение со ссылкой
     * @throws Exception если получение ссылки завершается ошибкой
     */
    public SendMessage handleSendTrack(UserData userData, long chatId) throws Exception {
        SendMessage outMessage = new SendMessage();
        outMessage.setChatId(String.valueOf(chatId));
        outMessage.setParseMode(ParseMode.HTML);
        DeezerMusicLink deezerMusicLink = new DeezerMusicLink();
        userData.changeUserState(chatId, UserState.WAITING_FOR_ARTISTS);
        System.out.println(trackList.get(chatId).get(index));
        String link = deezerMusicLink.getDeezerMusicLink(trackList.get(chatId).get(index));
        outMessage.setText(link);
        outMessage.setReplyMarkup(buttons.getKeyboardForUser(chatId, userData));
        return outMessage;
    }
}
