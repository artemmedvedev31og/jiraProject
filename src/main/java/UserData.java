import java.util.HashMap;
import java.util.Map;

/**
 * @brief Хранилище пользователей Telegram и их состояний в памяти приложения.
 *
 * Данные организованы в отображение «идентификатор чата → пользователь». Класс
 * используется обработчиком меню для определения текущего сценария диалога.
 */
public class UserData {
    /** Пользователи, зарегистрированные в текущем процессе приложения. */
    private final Map<Long, User> users = new HashMap<>();

    /**
     * @brief Проверяет, зарегистрирован ли пользователь.
     * @param userId идентификатор Telegram-пользователя/чата
     * @return {@code true}, если пользователь уже присутствует в хранилище
     */
    public boolean checkUser(long userId) {
        return users.containsKey(userId);
    }

    /**
     * @brief Добавляет пользователя с состоянием по умолчанию.
     * @param chatId идентификатор Telegram-чата
     */
    public void addUser(long chatId) {
        users.put(chatId, new User(chatId));
    }

    /**
     * @brief Возвращает текущее состояние пользователя.
     * @param chatId идентификатор Telegram-чата
     * @return состояние пользователя или {@code null}, если пользователь не найден
     */
    public UserState checkUserState(long chatId) {
        if(!users.containsKey(chatId)) {
            return null;
        }
        return users.get(chatId).getState();
    }

    /**
     * @brief Изменяет состояние пользователя.
     * @param chatId идентификатор Telegram-чата
     * @param userState новое состояние диалога
     */
    public void changeUserState(long chatId, UserState userState) {
        users.put(chatId, new User(chatId, userState));
    }

    /**
     * @brief Возвращает отображение всех пользователей.
     * @return отображение идентификаторов чатов и объектов пользователей
     */
    public Map<Long, User> getUsers() {
        return users;
    }
}
