/**
 * @brief Один элемент поисковой выдачи Genius API.
 *
 * Объект содержит поле {@code result}, в котором находятся сведения о найденной
 * композиции.
 */
public class Hits {
    /** @return данные найденной композиции */
    public Result getResult() {
        return result;
    }

    /** @param result данные найденной композиции */
    public void setResult(Result result) {
        this.result = result;
    }

    /** Данные композиции из свойства {@code result}. */
    private Result result;
}
