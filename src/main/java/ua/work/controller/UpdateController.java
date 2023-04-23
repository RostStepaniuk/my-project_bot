package ua.work.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
//import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ua.work.dao.UserDataRepository;
import ua.work.dao.UserLessonsRepository;
import ua.work.dao.entity.UserData;
import ua.work.dao.entity.UserLessons;
import ua.work.service.AudioService;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Log4j
@Component
public class UpdateController {

    private TelegramBot telegramBot;

    private final AudioService audioService;
    private final UserDataRepository userDataRepository;
    private final UserLessonsRepository userLessonsRepository;



    public UpdateController(AudioService audioService, UserDataRepository userDataRepository,
                            UserLessonsRepository userLessonsRepository) {
        this.audioService = audioService;
        this.userDataRepository = userDataRepository;
        this.userLessonsRepository = userLessonsRepository;
    }

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
        botCommand();
    }
    static final String HELP_TEXT = "это бот автоматически присылающий аудио лекции каждый день." +
            "\n\nВы можете оформить подписку на месяц и получать материал прямо сюда." +
            "\n\nПо истечении срока подписки, уроки будут недоступны и услуга приостанавлявается." +
            "\n\nтестовые команды: " +
                "\n/all_files - получить все файлы из папки" +
                "\n/get_lesson - получить урок сразу" +
                "\n/change_status - смена статуса";

    static final String filePath = "C:\\Users\\hokage\\Downloads\\Telegram Desktop\\lessons_mp3\\lesson1.mp3";
    static final String folderPath2 = "C:\\Users\\hokage\\Downloads\\Telegram Desktop\\lessons_mp3";
    static final String folderPath = "/opt/files/lesson_ogg";

    static final String congrats = "Поздравляю Вас! \n\nПодписка была успешно оформленна!" +
            "\n\nВот Ваш первый урок. \n\nВы будете получать каждый день новую порцию знаний!" +
            "\n\nВсе полученные лекции будут находиться в нижнем меню под кнопкой \uD83D\uDCDAмои уроки\uD83D\uDCDA" +
            "\n\nЖелаю удачи!☺";
    static final String comeback = "Поздравляю с продлением подписки!" +
            "\n\nВы большой молодец, что не престаете учиться и самосовершенствоваться!" +
            "\n\nДальше будет только интереснее\uD83D\uDE01✨";
    public void botCommand() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "переход в главное меню"));
        listOfCommands.add(new BotCommand("/help", "помощь")); //появятся кнопки по типу : как оплатить. сколько длится курс. буду ли я успевать.
        listOfCommands.add(new BotCommand("/question", "вопрос администратору"));//возможно ссылка на заказчика или где чел может написать и спросить что угодно

        telegramBot.executeBotCommand(listOfCommands);
    }

    public void processUpdate(Update update) {
        //это метод тг бота onUpdateReceived
        //он отвечает за  выполнение всех поступающих строк в боте

        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            log.info(update.getMessage().getText());


            switch (messageText) {
                case "/start":
                case "/menu":
                    userState(update.getMessage());
                    sendsMessage(chatId, "Главное меню:");  // это значит что чувак уже есть в базе
                    greetings(chatId, "Прошу, ознакомьтесь подробнее с возможностями бота\uD83D\uDE0A");
                    break;

                case "/help":
                    prepareAndSendMessage(chatId, HELP_TEXT);
                    break;

                case "\uD83C\uDF81меню\uD83C\uDF81":
                    userState(update.getMessage());
                    greetings(chatId, "Прошу, ознакомьтесь подробнее с возможностями бота\uD83D\uDE0A");
                    break;

                case "📚мои уроки📚":
                case "/lectures":
                    alreadyHavingSubscriptionWindow(chatId, "Открыть доступные вам уроки?");
                    break;

                case "\uD83D\uDC8Eмоя подписка\uD83D\uDC8E":
                    mySubscription(chatId);
                    break;

                case "все файлы":
                case "Все файлы":
                case "/all_files":
                    sendAllAudioFiles(chatId);
                    break;//dance


                case "/get_lesson":
                case "get lesson":
                case "получить урок":
                case "Получить урок":
                    sendLessonByDB(chatId);
                    break;

                case "обновить статус":
                case "/change_status":
                    changeSubscription(chatId);
                    break;
            }

        } else if (update.hasCallbackQuery()) {
            buttonImplementation(update, update.getMessage());
        }
    }

    private boolean userState(Message message) {
        boolean userInRepository = true;
        if(userDataRepository.findById(message.getChatId()).isEmpty()){
            userInRepository = false;
            registerUser(message);
        }
        return userInRepository;
    }
    private void registerUser(Message message) {
        if(userDataRepository.findById(message.getChatId()).isEmpty()){

            var chatId = message.getChatId();
            var chat = message.getChat();

            UserData user = new UserData();

            user.setChatId(chatId);
            user.setUserName(chat.getUserName());

            userDataRepository.save(user);
            log.info("ПОЛЬЗОВАТЕЛЬ СОХРАНЕН: " + user);
        }
    }

    //  метод дает тру статус доступа
    protected void accessGranted(long chatId) {
        UserData user = userDataRepository.findById(chatId).orElseThrow();

        if (user.isAccessGranted()){//если юзер уже имеент подиску
            alreadyHavingSubscriptionWindow(chatId, "Подписка действительна " + validity(chatId) +
                    "\n\nМожете открыть все ваши доступные уроки\uD83D\uDC47");
            return;
        }
        //обновляем ему статус если он фолз
        user.setAccessGranted(true);
        user.setActivationDate(new Timestamp(System.currentTimeMillis()));

        //если юзер оформляется первый раз
        if(user.getNumberLesson()==null){
            user.setNumberLesson(1);
            userDataRepository.save(user);
            System.out.println("\n(проверка на цикл)\n");
            firstSubscriptionWindow(chatId, congrats);

        } else {
            //если юзер покупает подписку вновь
            userDataRepository.save(user);
            System.out.println("\n(проверка на цикл)\n");
            sendLessonWindow(chatId, comeback);
        }

    }
    //клавиатура под вводом текста внизу
    private void sendsMessage(long chatId, String answer) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("\uD83C\uDF81меню\uD83C\uDF81");

        keyboardRows.add(row);

        row = new KeyboardRow(); //вторая строка кнопок

        row.add("\uD83D\uDC8Eмоя подписка\uD83D\uDC8E");
        row.add("\uD83D\uDCDAмои уроки\uD83D\uDCDA");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }
    private void executeMessage(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    void prepareAndSendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }
    //  окно менюшки в чате
    private void greetings(long chatId , String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("Что даёт этот бот\uD83D\uDE4C");
        firstButton.setCallbackData("преимущества");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);

        rowInLine = new ArrayList<>();

        InlineKeyboardButton thirdButton = new InlineKeyboardButton();
        thirdButton.setText("Программа курса\uD83D\uDCDA");
        thirdButton.setCallbackData("содержание");

        rowInLine.add(thirdButton);
        rowsInLine.add(rowInLine);

            rowInLine = new ArrayList<>();

        InlineKeyboardButton secondButton = new InlineKeyboardButton();
        secondButton.setText("Оформление подписки\uD83E\uDE99");
        secondButton.setCallbackData("подписка");


        rowInLine.add(secondButton);
        rowsInLine.add(rowInLine);

            rowInLine = new ArrayList<>();

        InlineKeyboardButton fourthButton = new InlineKeyboardButton();
        fourthButton.setText("Скрыть окно ↩");
        fourthButton.setCallbackData("скрыть");

        rowInLine.add(fourthButton);
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }
    //кнопка назад под текстом
    private void responseWindow(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("↩назад");
        firstButton.setCallbackData("назад");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }

    private void testPayment(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("оплатить (тестовая кнопка)");
        firstButton.setCallbackData("тест оплата");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }
    //клава для оплаты и возвращения назад
    private void subscriptionWindow(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("перейти к оплате");
        firstButton.setCallbackData("оплата");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);

        rowInLine = new ArrayList<>();

        InlineKeyboardButton secondButton = new InlineKeyboardButton();
        secondButton.setText("↩назад");
        secondButton.setCallbackData("назад");

        rowInLine.add(secondButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }
    // метод открывает все лекции доступные ученику
    private void alreadyHavingSubscriptionWindow(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("Открыть\uD83D\uDCDA");
        firstButton.setCallbackData("открыть мои лекции");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);

        rowInLine = new ArrayList<>();

        InlineKeyboardButton secondButton = new InlineKeyboardButton();
        secondButton.setText("↩назад");
        secondButton.setCallbackData("назад");

        rowInLine.add(secondButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }
    private void sendLessonWindow(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("Получить");
        firstButton.setCallbackData("получить урок");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }

    private void lastLessonWindow(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("Получить");
        firstButton.setCallbackData("последний урок");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }

    private void firstSubscriptionWindow(long chatId, String text) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton firstButton = new InlineKeyboardButton();
        firstButton.setText("открыть \uD83D\uDC47");
        firstButton.setCallbackData("получить урок");

        rowInLine.add(firstButton);
        rowsInLine.add(rowInLine);


        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }

    private void buttonImplementation(Update update, Message message) {
        String callbackData = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String pen = "\uD83D\uDCCC";

        if (callbackData.equals("преимущества")) {

            String text = HELP_TEXT;
            responseWindow(chatId, text);
            executeEditMessageTest(pen, chatId, messageId);


        } else if (callbackData.equals("содержание")) {

            String text = "1.Здесь информация о чем курс, что нас ждет \n\n2.что мы получим " +
                    " \n\n3.почему он необходим и кому он подойдет ";
            responseWindow(chatId, text);
            executeEditMessageTest(pen, chatId, messageId);

        } else if (callbackData.equals("подписка")) {
            //проверка на статус
            UserData user = userDataRepository.findById(chatId).orElseThrow();
            if (user.isAccessGranted()){
                accessGranted(chatId);//прожать на подписку в окне
                executeEditMessageTest(pen, chatId, messageId);
                return;
            }

            String text = "Подписка действует месяц и стоит 100 баксов. " +
                    "\n\nЗдесь соответсвенно описание";
            subscriptionWindow(chatId, text);
            executeEditMessageTest(pen, chatId, messageId);

        } else if (callbackData.equals("скрыть")) {

            executeEditMessageTest("↩", chatId, messageId);
        } else if (callbackData.equals("назад")) {

            executeEditMessageTest("↩", chatId, messageId);
            greetings(chatId, "Прошу, ознакомьтесь подробнее с возможностями бота\uD83D\uDE0A");

        } else if (callbackData.equals("оплата")) {

            executeEditMessageTest("↩", chatId, messageId);
            purchase(chatId);//кнопка реальной оплаты
            testPayment(chatId, "это кнопка для тестирования.\n\nв дальнейшем ее не будет");

        } else if (callbackData.equals("тест оплата")) {

            executeEditMessageTest("↩", chatId, messageId);

            accessGranted(chatId);//тестовая оплата

        } else if (callbackData.equals("открыть мои лекции")) {

            executeEditMessageTest("↩", chatId, messageId);
            sendAccessLessons(chatId);

        } else if (callbackData.equals("получить урок")) {

            executeEditMessageTest("↩", chatId, messageId);
            sendLessonByDB(chatId);

        } else if (callbackData.equals("последний урок")) {

            executeEditMessageTest("↩", chatId, messageId);
            lastLesson(chatId);
        }

    }
    //этот метод отвечает на кнопки инлайн клавиатуры и сразу же удаляет свой текст
    private void executeEditMessageTest(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        telegramBot.executeMessageText(message);

        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(String.valueOf(chatId));
        deleteMessage.setMessageId((int) messageId);

        telegramBot.deleteMessage(deleteMessage);
    }

    //метод для загрузки аудио из папки в Базу
    private void setFileIntoDB(long chatId){
        String directoryPath = "C:\\Users\\hokage\\Downloads\\Telegram Desktop\\lessons_mp3";
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                String filePath = file.getAbsolutePath();
                UserLessons userLessons = userLessonsRepository.findByFilePath(filePath);
                if (userLessons == null) {
                    audioService.saveAudio(fileName, filePath);
                    System.out.println("Файл " + fileName + " загружен в базу данных.");
                    prepareAndSendMessage(chatId, "Файл " + fileName + " загружен в базу данных.");
                } else {
                    System.out.println("Файл " + fileName + " уже существует в базе данных.");
                    prepareAndSendMessage(chatId, "Файл " + fileName + " уже существует в базе данных.");
                }
            }
        }
    }

    //метод загружающий все аудио из папки
    private void sendAllAudioFiles(long chatId) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mp3")) {
                    InputFile inputFile = new InputFile(file);
                    SendAudio sendAudio = new SendAudio(String.valueOf(chatId), inputFile);
                    sendAudio.setTitle(file.getName());
                    telegramBot.executeAudio(sendAudio);
                }
            }
            prepareAndSendMessage(chatId, "Все файлы высланны, красавчик\uD83D\uDE0E");
            System.out.println("Все файлы высланны, красавчик");
        } else {
            System.out.println("Папка не найдена");
        }
    }
    //метод отправляющий урок конкретного номера
    private void sendLessonAudio(long chatId, int lessonNumber) {
        // Получаем имя файла с аудио для данного номера урока
        String fileName = String.format("lesson%01d.mp3", lessonNumber);

        // Создаем объект InputFile из файла на диске
        InputFile inputFile = new InputFile(new File(folderPath, fileName));

        // Создаем объект SendAudio с параметрами аудио
        SendAudio sendAudio = new SendAudio(String.valueOf(chatId), inputFile);
        sendAudio.setPerformer("performer");
        sendAudio.setTitle(fileName);

        // Отправляем аудио-файл на сервер телеграм
        telegramBot.executeAudio(sendAudio);
    }

//метод отправляющий уроки отталкиваясь от БД

    void sendLessonByDB(long chatId){
        Optional<UserData> userDataOptional = userDataRepository.findById(chatId);

        if(userDataOptional.isPresent()) {
            UserData userData = userDataOptional.get();
            Integer lessonNumber = userData.getNumberLesson();

            if(lessonNumber == null){
                return;
            }

            if(allForToday(chatId)){
                System.out.println("на сегодня " + userData.getUserName() + " получил все уроки");
                return;
            }


            String fileName = String.format("lesson%01d.mp3", lessonNumber);
            File file = new File(folderPath, fileName);

            if (!file.exists()) {
                System.out.println("Файл не найден: " + fileName);
                prepareAndSendMessage(chatId, "на текущий момент не осталось уроков");
                return;
            }

            sendLessonAudio(chatId, lessonNumber);
            userData.setDispatchTime(new Timestamp(System.currentTimeMillis()));
            userData.setNumberLesson(lessonNumber + 1);
            userDataRepository.save(userData);
        } else {
            System.out.println("Пользователь с chatId " + chatId + " не найден");
        }
    }
    //метод высылающий все доступные уроки
    private void sendAccessLessons(long chatId) {
        Optional<UserData> userDataOptional = userDataRepository.findById(chatId);

        if(userDataOptional.isPresent()) {
            UserData userData = userDataOptional.get();
            Integer currentLessonNumber = userData.getNumberLesson();

            UserData user = userDataRepository.findById(chatId).orElseThrow();
            if (currentLessonNumber == null){
                prepareAndSendMessage(chatId, "У Вас еще нет ни одного урока\uD83E\uDD17");
                return;
            }
            if (!user.isAccessGranted()){
                prepareAndSendMessage(chatId, "К сожалению срок подписки подошел к концу\uD83E\uDD7A");
                return;
            }

            for (int i = 1; i < currentLessonNumber; i++) {
                String fileName = String.format("lesson%01d.mp3", i);
                File file = new File(folderPath, fileName);

                if (!file.exists()) {
                    System.out.println("Файл не найден: " + fileName);
                    continue;
                }

                sendLessonAudio(chatId, i);
            }
        } else {
            System.out.println("Пользователь с chatId " + chatId + " не найден");
            prepareAndSendMessage(chatId, "вас нет в базе данных\uD83E\uDEE5");
        }
    }

    private void lastLesson(long chatId){
        UserData userData = userDataRepository.findById(chatId).orElseThrow();
        Integer currentLessonNumber = userData.getNumberLesson();
        if (currentLessonNumber == null){
            prepareAndSendMessage(chatId, "У Вас еще нет ни одного урока\uD83E\uDD17");
            return;
        }
        if (!userData.isAccessGranted()){
            prepareAndSendMessage(chatId, "К сожалению срок подписки подошел к концу\uD83E\uDD7A");
            return;
        }
        sendLessonAudio(chatId, currentLessonNumber - 1);
    }



    // @Scheduled(cron = "0 */5 * * * ?")//каждые 5 минут
    @Scheduled(cron = "0 0 5 * * ?") // запускать каждый день в 5:00 утра
    public void sendFileEverydayAtFiveAM2() {
        List<UserData> userDataList = userDataRepository.findAll();
        for (UserData userData : userDataList) {

            Integer lessonNumber = userData.getNumberLesson();
            if(lessonNumber == null && userData.isAccessGranted()){
                userData.setNumberLesson(1);
                userDataRepository.save(userData);
            }

            if(userData.isAccessGranted()) {

                if(allForToday(userData.getChatId())){
                    System.out.println("на сегодня " + userData.getUserName() + " получил все уроки");
                    continue;
                }
                userData.setNumberLesson(lessonNumber + 1);
                userData.setDispatchTime(new Timestamp(System.currentTimeMillis()));
                userDataRepository.save(userData);

                lastLessonWindow(userData.getChatId(), "Вы получили новый урок!\n" +
                        "Открыть?");
                System.out.println("урок выслан юзеру " + userData.getUserName());

            } else {
                System.out.println("ПОЛЬЗОВАТЕЛЬ "+ userData.getChatId() +"  "+ userData.getUserName() +" без подписки");
                continue;
            }

            System.out.println("автоматическая рассылка метод был активирован");

            if (expired(userData.getChatId())){
                System.out.println("подписка закончилась у " + userData.getUserName() +" с айди: "+ userData.getChatId());
                break;
            }
        }
    }


    //метод под кнопкой "моя подписка"
    //это окно вылазит после прожатия кнопки (оформление подписки)
    private void mySubscription(long chatId){
        UserData user = userDataRepository.findById(chatId).orElseThrow();

        if(!user.isAccessGranted()){

            greetings(chatId, "У вас не активирована подписка\uD83E\uDD7A" +
                    "\n\nОзнакомьтесь подробнее с возможностями оплаты\uD83D\uDE0C");
        } else {
            if(!expired(chatId))
            accessGranted(chatId);//нижняя кнопка "моя подиска"
        }
    }

    private void changeSubscription(long chatId){
        UserData user = userDataRepository.findById(chatId).orElseThrow();

        if(!user.isAccessGranted()){
            user.setAccessGranted(true);
            userDataRepository.save(user);
            prepareAndSendMessage(chatId, "Ваша подписка теперь активна\uD83D\uDE01");
        } else {
            prepareAndSendMessage(chatId, "Подписка неактивна\uD83D\uDE06");
            user.setAccessGranted(false);
            userDataRepository.save(user);
        }
    }

    private void purchase(long chatId){
        SendInvoice sendInvoice = new SendInvoice();
        sendInvoice.setChatId(String.valueOf(chatId));
        sendInvoice.setTitle("уроки Кулешова");
        sendInvoice.setDescription("Оформление подписки");
        sendInvoice.setPayload("lessons");
        sendInvoice.setProviderToken("632593626:TEST:sandbox_i92420978458");
        sendInvoice.setCurrency("USD");
        sendInvoice.setPrices(new ArrayList<>(Collections.singletonList(
                new LabeledPrice("Подписка Coach Bot ", 50) // цена в копейках
        )));
        telegramBot.executePurchase2(sendInvoice, chatId);
    }

    private boolean expired(long chatId){
        UserData userData = userDataRepository.findById(chatId).orElseThrow();
        Timestamp activationDate = userData.getActivationDate();
        LocalDate currentDate = LocalDate.now();
        LocalDate activationLocalDate = activationDate.toLocalDateTime().toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(activationLocalDate, currentDate);

        if (daysBetween > 30) {
            userData.setAccessGranted(false);
            userDataRepository.save(userData);
            greetings(chatId, "К сожалению срок подписки подошел к концу.\uD83E\uDD7A" +
                    "\n\nНо в любой удобный момент Вы можете ее продлить вновь\uD83D\uDE0A");
            return true;
        }
        return false;
    }

    private boolean allForToday(long chatId){
        UserData userData = userDataRepository.findById(chatId).orElseThrow();
        if(userData.getNumberLesson()== 1) {
            return false;
        }
        if(userData.getDispatchTime() == null) {
            userData.setDispatchTime(new Timestamp(System.currentTimeMillis()));
            userDataRepository.save(userData);
            return false;
        }
        Timestamp dispatchDate = userData.getDispatchTime();
        LocalDate currentDate = LocalDate.now();
        LocalDate dispatchLocalDate = dispatchDate.toLocalDateTime().toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(dispatchLocalDate, currentDate);

        if (daysBetween < 1) {
            return true;
        }
        return false;
    }

    private String validity(long chatId) {
        UserData userData = userDataRepository.findById(chatId).orElseThrow();
        Timestamp activationDate = userData.getActivationDate();
        LocalDate currentDate = LocalDate.now();
        LocalDate activationLocalDate = activationDate.toLocalDateTime().toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(activationLocalDate, currentDate);
        String result = " еще "+ (30 - daysBetween) + " дней\uD83D\uDE0C";
        return result;
    }
}