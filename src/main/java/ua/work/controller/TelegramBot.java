package ua.work.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
//import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.work.configuration.BotConfig;
import ua.work.dao.UserDataRepository;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.List;

@Log4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final UserDataRepository userDataRepository;
    final BotConfig config;
    private final UpdateController updateController;


    public TelegramBot(UserDataRepository userDataRepository, BotConfig config, UpdateController updateController) {
        this.userDataRepository = userDataRepository;
        this.config = config;
        this.updateController = updateController;


    }

    @PostConstruct
    public void init() {
        updateController.registerBot(this);
    }


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
    }


    public void sendAnswerMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка где-то в executeMessage()" + e.getMessage());
        }
    }

    public void executeMessageText(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error("ERROR_TEXT" + e.getMessage());
        }
    }


    public void executeBotCommand(List<BotCommand> listOfCommands) {
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }


    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Error deleting message: " + e.getMessage());
        }
    }
    //это метод что загружает в базу все файлы из указанной папки


    public void executeAudio(SendAudio sendAudio) {
        // Отправляем аудио-файл на сервер телеграм
        try {
            execute(sendAudio);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
//    @PostConstruct
//    @Transactional
//    public void updateAllRunningCycleToFalse() {
//        userDataRepository.updateAllRunningCycleToFalse();
//    }


    //этот метод вроде бы работает. нужно токо платёжку подключить нормальную
    void executePurchase2(SendInvoice sendInvoice, long chatId) {
        try {
            Message message = execute(sendInvoice);
            if (message.getSuccessfulPayment() != null)
                updateController.accessGranted(chatId);

        } catch (TelegramApiException e) {
            e.printStackTrace();
            // обработка ошибки
        }
    }

}
