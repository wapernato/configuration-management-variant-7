# Этап 5. Дополнительные команды, unit-тесты и Codespaces

Вариант 7, Java 17+. Реализованы `cp` и `mkdir`. Все изменения относятся
к VFS в памяти: исходный CSV и файлы компьютера остаются прежними.
Команды запуска выполняются из корня проекта.

## Запуск и демонстрация

```sh
./run.sh --vfs examples/stage3/deep.csv
./scripts/demo-stage5.sh
./scripts/demo-stage5.sh cp-descendant
./scripts/demo-stage5.sh mkdir-rollback
```

| Команда | Режим |
| --- | --- |
| `mkdir [--] пути...` | Создать один или несколько каталогов; родители должны существовать |
| `mkdir -p [--] пути...` | Создать родителей; существующий каталог не является ошибкой |
| `cp [--] источники... назначение` | Копировать файл; существующий файл перезаписывается |
| `cp -r` / `cp -R` | Рекурсивно копировать каталог, включая двоичные и пустые файлы |
| `cp -n` | Сохранять существующие файлы назначения, не перезаписывая их |
| `cp -rn` | Рекурсивная копия с сохранением существующих файлов |

Опции можно объединять. `--` завершает их разбор. Пути в кавычках,
абсолютные/относительные пути, `.`, `..` и `~` работают как в этапе 4.
Несколько источников `cp` допустимы только при существующем каталоге
назначения. При копировании в каталог добавляется исходное имя.
При рекурсивном копировании в существующее дерево каталоги объединяются.

Ошибки: отсутствующий источник/родитель, неверные опции/аргументы,
конфликт файла и каталога, копия в себя, каталог без `-r`, рекурсивная
копия внутрь исходного дерева. Первая ошибка отменяет **всю команду**,
включая ранее обработанные операнды. Для этого команда работает на копии
карты узлов и публикует её только после успеха. Изменения не сохраняются
между сеансами; повторная загрузка CSV возвращает исходное дерево.

## Unit-тесты JUnit 5

Maven Wrapper включён в репозиторий (Apache Maven 3.9.11, проверка SHA-256).
Отдельная установка Maven не требуется. Для первого запуска нужен интернет,
для локальной сборки нужен JDK 17+. Windows: команды `mvnw.cmd` запускаются
из PowerShell или cmd; macOS/Linux: `./mvnw`.

```sh
./mvnw test -Dtest=Stage5Test  # только unit-тесты этапа 5
./mvnw verify                # JUnit, регрессия и переносимый JAR
java -jar target/variant7-terminal.jar --vfs examples/stage3/deep.csv
```

Windows:

```powershell
.\mvnw.cmd test -Dtest=Stage5Test
.\mvnw.cmd verify
java -jar target/variant7-terminal.jar --vfs examples/stage3/deep.csv
```

`Stage5Test` использует новую VFS для каждого теста. Проверяет:

- создание каталогов, `-p`, несколько путей, кавычки, `--`, относительные пути;
- копию файлов, перезапись/`-n`, несколько источников, `-r`/`-R` и слияние деревьев;
- точное сохранение двоичных данных и пустых файлов, независимость копии;
- ошибки типов, отсутствующие пути и запрет копирования внутрь себя;
- откат всей команды после ошибки, включая несколько источников;
- неизменность реального CSV и папки с помощью JUnit `@TempDir`.

`RegressionTest` подключает проверки прежних этапов к Maven. Отчёты
JUnit: `target/surefire-reports/`. `./scripts/test.sh` проверяет команды
и все стартовые сценарии без скачивания JUnit; с `GUI_TESTS=1` также
проверяется настоящее окно Swing, создание/копия и продолжение после ошибки.

Локальная проверка: 43 unit-теста этапа 5 и 4 набора регрессионных тестов
прошли через `./mvnw clean verify`. Отдельно прошли 303 проверки,
включая настоящее окно Swing. Собранный JAR запускает успешный сценарий
из другой папки и завершает его с кодом 0; исходный CSV остаётся прежним.

## GitHub Codespaces: GUI в браузере

`.devcontainer/devcontainer.json` устанавливает Java 17 и официальный
`desktop-lite`: это рабочий стол для графических приложений через noVNC.
При создании окружения выполняется `./mvnw verify`, включая unit-тесты,
и собирается `target/variant7-terminal.jar`.

1. После публикации коммитов откройте репозиторий на GitHub.
2. Нажмите **Code → Codespaces → Create codespace on main**.
3. Дождитесь установки контейнера и завершения `postCreateCommand`.
4. В терминале Codespaces выполните `./scripts/start-codespaces.sh`.
5. Откройте вкладку **Ports**, найдите **6080**, нажмите **Open in Browser**.
6. В noVNC нажмите **Connect**, пароль рабочего стола по умолчанию: `vscode`.
7. Появится окно Java-терминала; управляйте командами прямо в браузере.

Порт 6080 используйте с видимостью **Private**. Для запуска успешного
сценария в браузере: `./scripts/start-codespaces.sh --startup-script
examples/stage5/startup-success.txt` (введите команду одной строкой).
В окружении уже есть JDK, поэтому на локальном компьютере достаточно
браузера и входа в GitHub с доступом к приватному репозиторию.
Конфигурация подготовлена; облачный запуск проверяется после публикации
коммитов и создания Codespace.

Основание конфигурации: [Dev Containers Java](https://github.com/devcontainers/images/tree/main/src/java),
[desktop-lite / noVNC](https://github.com/devcontainers/features/tree/main/src/desktop-lite),
[порты Codespaces](https://docs.github.com/en/codespaces/developing-in-a-codespace/forwarding-ports-in-your-codespace).

## Стартовые сценарии и защита

`examples/stage5/startup-success.txt` показывает режимы команд, сохранение
и перезапись копии через `wc`, рекурсивную копию трёх уровней и навигацию.
Отдельные ошибочные сценарии останавливаются до строки `NEVER_EXECUTED`.

```sh
./scripts/demo-stage5.sh mkdir-parent
./scripts/demo-stage5.sh mkdir-existing
./scripts/demo-stage5.sh mkdir-file
./scripts/demo-stage5.sh mkdir-options
./scripts/demo-stage5.sh mkdir-arguments
./scripts/demo-stage5.sh cp-missing
./scripts/demo-stage5.sh cp-directory
./scripts/demo-stage5.sh cp-self
./scripts/demo-stage5.sh cp-parent
./scripts/demo-stage5.sh cp-conflict
./scripts/demo-stage5.sh cp-options
./scripts/demo-stage5.sh cp-arguments
./scripts/demo-stage5.sh cp-multiple
./scripts/demo-stage5.sh cp-rollback
./scripts/demo-stage5.sh empty-path
./scripts/demo-stage5.sh quotes
```

На защите: `mkdir -p demo/one/two`, `cp hello.txt demo/one/two`,
`ls demo/one/two`, `wc demo/one/two/hello.txt`, `cp -r a tree`,
`ls tree/b/c`; затем покажите ошибку `cp -r a a/b` и unit-тесты.
После ошибочного сценария вручную завершите окно командой `exit`.
