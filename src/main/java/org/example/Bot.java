package org.example;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jeasy.states.api.*;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.io.IOException;
import java.util.*;



public class Bot extends TelegramLongPollingBot {


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage message = new SendMessage();
            message.setChatId(update.getMessage().getChatId().toString());
            String text = update.getMessage().getText().toLowerCase();
            try {
                recogniseCommand(text, message);
            } catch (FiniteStateMachineException e) {
                throw new RuntimeException(e);
            }

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "titmoningbot_test";
    }

    @Override
    public String getBotToken() {
        return "6185420111:AAG2DG-CQ9nIS3wqO5gjkMkVqcXv3_LbWS4";
    }

    public void recogniseCommand(String text, SendMessage message) throws FiniteStateMachineException {
        setButtons(message);
        /*if(text.equals("������") || text.equals("�������") || text.equals("hi")){
            message.setText("������! ��� ����?");
        }
        if(text.equals("������") || text.equals("����� ������?")){
            message.setText("� ����� ������ ������ ������ ������?");

        }
        if(text.equals("�������� � ����") || text.equals("��� ��?")){
            message.setText("� �������������� ��� - ��� � ������.");
        }
        if(text.equals("������")){
            String URL = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=cccf006b67c53e10fb5a4d501d49c392",text);
            message.setText(Nemain.getResponse(URL));
        }*/
        String state = turnstileStateMachine.getCurrentState().getName();
        System.out.println(state);
        System.out.println(text);
        if (state.equals("���������") && text.equals("�������� � ������")) {
            message.setText("� ����� ������?");
            this.turnstileStateMachine.fire(new transitionToWeatherEvent());
            return;
        }
        if (state.equals("���������") && text.equals("�������� � ����")) {
            message.setText("� �������������� ���-��� � ������!");
            this.turnstileStateMachine.fire(new infoBotRequestEvent());
            return;
        }
        if (state.equals("������") && text.equals("�������� � ����")) {
            this.turnstileStateMachine.fire(new transitionFromWeatherToInfoEvent(message));
            return;
        }
        //if (state.equals("������") && text.equals("�������� � ������")) {
           // this.turnstileStateMachine.fire();
       // }
        if(state.equals("����") && text.equals("�������� � ������")){
            this.turnstileStateMachine.fire(new transitionFromInfoToWeatherEvent(message));
            return;
        }
        if (state.equals("������")) {
            this.turnstileStateMachine.fire(new weatherRequestEvent(text, message));
        }
        state = turnstileStateMachine.getCurrentState().getName();
        System.out.println(state);
    }

    public synchronized void setButtons(SendMessage sendMessage) {
        // ������� �����������
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        // ������� ������ ����� ����������
        List<KeyboardRow> keyboard = new ArrayList<>();

        // ������ ������� ����������
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        // ��������� ������ � ������ ������� ����������
        keyboardFirstRow.add(new KeyboardButton("�������� � ����"));

        // ������ ������� ����������
        KeyboardRow keyboardSecondRow = new KeyboardRow();
        // ��������� ������ �� ������ ������� ����������
        keyboardSecondRow.add(new KeyboardButton("�������� � ������"));

        // ��������� ��� ������� ���������� � ������
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        // � ������������� ���� ������ ����� ����������
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    static class transitionToWeatherEvent extends AbstractEvent {
        public transitionToWeatherEvent() {
            super("transitionToWeatherEvent");
        }
    }

    static class weatherRequestEvent extends AbstractEvent {
        String text;
        SendMessage message;

        public weatherRequestEvent(String text, SendMessage message) {
            super("weatherRequestEvent");
            this.text = text;
            this.message = message;
        }
    }

    static class infoBotRequestEvent extends AbstractEvent {
        public infoBotRequestEvent() {
            super("infoBotRequestEvent");
        }
    }

    static class transitionFromWeatherToInfoEvent extends AbstractEvent {
        SendMessage message;

        public transitionFromWeatherToInfoEvent(SendMessage message) {
            super("transitionFromWeatherToInfoEvent");
            this.message = message;
        }
    }

    static class transitionFromInfoToWeatherEvent extends AbstractEvent {
        SendMessage message;

        public transitionFromInfoToWeatherEvent(SendMessage message) {
            super("transitionFromInfoToWeatherEvent");
            this.message = message;
        }
    }


    public Bot() {
        State startingState = new State("���������");
        State weatherState = new State("������");
        State infoBotState = new State("����");


        Set<State> states = new HashSet<>();
        states.add(startingState);
        states.add(weatherState);
        states.add(infoBotState);


        Transition weather = new TransitionBuilder()
                .name("������")
                .sourceState(startingState)
                .eventType(transitionToWeatherEvent.class)
                .eventHandler(new transitionToWeather())
                .targetState(weatherState)
                .build();

        Transition weatherRequest = new TransitionBuilder()
                .name("������ ������")
                .sourceState(weatherState)
                .eventType(weatherRequestEvent.class)
                .eventHandler(new weatherRequest())
                .targetState(weatherState)
                .build();

        Transition infoRequest = new TransitionBuilder()
                .name("����")
                .sourceState(startingState)
                .eventType(infoBotRequestEvent.class)
                .eventHandler(new infoBotRequest())
                .targetState(startingState)
                .build();

        Transition fromWeatherToInfo = new TransitionBuilder()
                .name("� ������ �� ����")
                .sourceState(weatherState)
                .eventType(transitionFromWeatherToInfoEvent.class)
                .eventHandler(new transitionFromWeatherToInfoRequest())
                .targetState(infoBotState)
                .build();

        Transition fromInfoToWeather = new TransitionBuilder()
                .name("� ������ �� ����")
                .sourceState(infoBotState)
                .eventType(transitionFromInfoToWeatherEvent.class)
                .eventHandler(new transitionFromInfoToWeatherRequest())
                .targetState(weatherState)
                .build();

        this.turnstileStateMachine = new FiniteStateMachineBuilder(states, startingState)
                .registerTransition(weather)
                .registerTransition(weatherRequest)
                .registerTransition(infoRequest)
                .registerTransition(fromWeatherToInfo)
                .registerTransition(fromInfoToWeather)
                .build();

    }


    FiniteStateMachine turnstileStateMachine;

    static class transitionToWeather implements EventHandler<transitionToWeatherEvent> {

        @Override
        public void handleEvent(transitionToWeatherEvent transitionToWeatherEvent) throws Exception {

        }
    }

    static class weatherRequest implements EventHandler<weatherRequestEvent> {
       OkHttpClient okHttpClient = new OkHttpClient();
        @Override
        public void handleEvent(weatherRequestEvent weatherRequestEvent) throws Exception {

            String URL = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=7aa3700f3c560278ddb2edaab1e9c8e9&lang=ru&units=metric", weatherRequestEvent.text);
            try {
                String result = "";
                Request request = new Request.Builder().url(URL).build();

                Response response = this.okHttpClient.newCall(request).execute();
                result = response.body().string();
                Object obj = new JSONParser().parse(result);
                JSONObject jo = (JSONObject) obj;
                String name = (String) jo.get("name");
                System.out.println(name);
                JSONObject coord = (JSONObject) jo.get("coord");
                double lat = (double) coord.get("lat");
                JSONArray weather = (JSONArray) jo.get("weather");
                JSONObject jo2 = (JSONObject) weather.get(0);
                String description = (String) jo2.get("description");
                System.out.println(description);
                System.out.println(lat);
                JSONObject main = (JSONObject) jo.get("main");
                double temp = (double) main.get("temp");
                weatherRequestEvent.message.setText("������ � " + name + ": " + description +", " + "�����������: " + temp + "�C.");
            } catch (IOException | ParseException e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }

        }
    }

    static class infoBotRequest implements EventHandler<infoBotRequestEvent> {

        @Override
        public void handleEvent(infoBotRequestEvent infoBotRequestEvent) throws Exception {

        }
    }


    static class transitionFromWeatherToInfoRequest implements EventHandler<transitionFromWeatherToInfoEvent> {

        @Override
        public void handleEvent(transitionFromWeatherToInfoEvent transitionFromWeatherToInfoEvent) throws Exception {
            transitionFromWeatherToInfoEvent.message.setText("� �������������� ���-��� � ������!");
        }
    }

    static class transitionFromInfoToWeatherRequest implements EventHandler<transitionFromInfoToWeatherEvent> {

        @Override
        public void handleEvent(transitionFromInfoToWeatherEvent transitionFromInfoToWeatherEvent) throws Exception {
            transitionFromInfoToWeatherEvent.message.setText("� ����� ������?");
        }
    }
}




