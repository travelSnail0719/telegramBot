package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class MyTelegramBot extends TelegramLongPollingBot {
    private static final String Url = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=%EC%97%94%ED%99%94";
    private static double targetRate = 0.0;
    private static double MaxtargetRate;
    private static boolean isSettingRate;
    private static MyTelegramBot bot;
    static ArrayList<Double> targetRateList = new ArrayList<Double>();
    private int lastProcessedUpdateId = 0;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String inputText = update.getMessage().getText();
            int updateId = update.getUpdateId();

            if(updateId == lastProcessedUpdateId){
                return;
            }

            if("/set_rate".equals(inputText)){
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(update.getMessage().getChatId()));
                message.setText("목표 환율을 입력해주세요.");
                isSettingRate = true;

                try{
                    execute(message);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                }
            } else if(isSettingRate) {
                try{
                    targetRate = Double.parseDouble(inputText);

                    DecimalFormat df = new DecimalFormat("#.##");
                    df.setRoundingMode(RoundingMode.HALF_UP);
                    String maxTargetRateStr = df.format(targetRate + 0.43);
                    MaxtargetRate = Double.parseDouble(maxTargetRateStr);

                    targetRateList.add(MaxtargetRate);
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(update.getMessage().getChatId()));
                    message.setText("목표 환율이 설정되었습니다. " +  System.lineSeparator() + " 매도 금액 : " + MaxtargetRate);
                    isSettingRate = false;

                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } catch (NumberFormatException e){
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(update.getMessage().getChatId()));
                    message.setText("목표 환율은 숫자로 입력해주세요.");
                    try{
                        execute(message);
                    } catch (TelegramApiException ex){
                        ex.printStackTrace();
                    }
                }
            }
            lastProcessedUpdateId = updateId;
        }
    }

    @Override
    public String getBotUsername() {
        // userName 입력
        return "";
    }

    @Override
    public String getBotToken() {
        // 텔레그램 botId 입력
        return "";
    }

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                bot = new MyTelegramBot();
                botsApi.registerBot(bot);
                Document doc = Jsoup.connect(Url).get();
                Elements rateElements = doc.select(".price_info .price");
                double rate = Double.parseDouble(rateElements.text());
                for(int i = 0; i < targetRateList.size(); i++){
                    if (rate >= targetRateList.get(i)) {
                        // 환율이 목표 금액에 도달했을 때 텔레그램 메시지를 보냅니다.
                        sendTelegramMessage("환율이 목표 금액에 도달했습니다." +  System.lineSeparator() + " 목표금액 : " +targetRateList.get(i) + " 현재환율 : " + rate);
                        targetRateList.remove(i);
                    }
                }
                isSettingRate = false;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.MINUTES); // 매 분마다 환율 확인.
    }

    private static void sendTelegramMessage(String Text){
        SendMessage message = new SendMessage();
        // chatId 입력
        message.setChatId("");
        message.setText(Text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
}
