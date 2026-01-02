package com.example.aiapp;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;

import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends Activity {

    private static final int PERMISSION_CODE = 100;

    private View voiceOrb;
    private ImageButton btnMic;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private TextToSpeech tts;

    private boolean isListening = false;
    private final Queue<String> ttsQueue = new LinkedList<>();

    // WhatsApp data
    public static String lastWhatsAppMessage;
    public static String lastWhatsAppSender;
    public static PendingIntent lastReplyIntent;
    public static RemoteInput lastRemoteInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceOrb = findViewById(R.id.voiceOrb);
        btnMic = findViewById(R.id.btnMic);

        requestPermissions();
        initSpeech();
        initTTS();

        btnMic.setOnClickListener(v -> startListening());
    }

    // ================= PERMISSIONS =================

    private void requestPermissions() {
        String[] perms = { Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG };

        ArrayList<String> list = new ArrayList<>();
        for (String p : perms) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                list.add(p);
            }
        }
        if (!list.isEmpty()) {
            requestPermissions(list.toArray(new String[0]), PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        for (int r : res) {
            if (r != PackageManager.PERMISSION_GRANTED) {
                speak("Tüm izinler gerekli");
                return;
            }
        }
    }

    // ================= SPEECH =================

    private void initSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list == null || list.isEmpty())
                    return;

                String cmd = list.get(0).toLowerCase();

                if (!handleCommand(cmd)) {
                    askAI(cmd);
                }
            }

            public void onError(int e) {
                isListening = false;
            }

            public void onReadyForSpeech(Bundle b) {
            }

            public void onBeginningOfSpeech() {
            }

            public void onRmsChanged(float rmsdB) {
                // Orb'un büyüklüğünü ses şiddetine göre değiştir
                float scale = 1.0f + (rmsdB / 10.0f);
                voiceOrb.setScaleX(scale);
                voiceOrb.setScaleY(scale);
            }

            public void onBufferReceived(byte[] b) {
            }

            public void onEndOfSpeech() {
            }

            public void onPartialResults(Bundle b) {
            }

            public void onEvent(int t, Bundle b) {
            }
        });
    }

    private void startListening() {
        if (!isListening) {
            isListening = true;
            speechRecognizer.startListening(speechIntent);
        }
    }

    // ================= COMMANDS =================

    private boolean handleCommand(String c) {

        // --- NIKO KİMLİK KONTROLÜ ---
        if (c.contains("adın ne") || c.contains("kimsin") || c.contains("kendini tanıt")) {
            speak("Benim adım Niko. Senin kişisel yapay zeka asistanınım.");
            return true;
        }

        if (c.contains("whatsapp") && c.contains("oku")) {
            readLastWhatsAppMessage();
            return true;
        }

        if (c.contains("whatsapp") && c.contains("cevap")) {
            replyWhatsApp("Tamam");
            return true;
        }

        if (c.contains("son gelen")) {
            callLast(CallLog.Calls.INCOMING_TYPE);
            return true;
        }

        if (c.contains("son aranan")) {
            callLast(CallLog.Calls.OUTGOING_TYPE);
            return true;
        }

        if (c.contains("ara")) {
            callByName(c.replace("ara", "").trim());
            return true;
        }

        return false;
    }

    // ================= CALL =================

    private void callLast(int type) {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            return;

        try (Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls.TYPE + "=?",
                new String[] { String.valueOf(type) }, CallLog.Calls.DATE + " DESC")) {

            if (c != null && c.moveToFirst()) {
                startCall(c.getString(c.getColumnIndex(CallLog.Calls.NUMBER)));
            }
        }
    }

    private void callByName(String name) {
        try (Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?", new String[] { "%" + name + "%" },
                null)) {

            if (c != null && c.moveToFirst()) {
                startCall(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            }
        }
    }

    private void startCall(String phone) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            return;

        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
    }

    // ================= AI =================

    private void askAI(String q) {
        new Thread(() -> {
            try {
                // 1. URL: Emülatör için 10.0.2.2, Gerçek cihaz için PC'nizin IP'si (örn.
                // 192.168.1.35)
                // Lütfen backend'in çalıştığı adresi buraya yazın:
                URL url = new URL("https://streets-doom-atmospheric-relaxation.trycloudflare.com/chat");

                // 2. Bağlantı Ayarları
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("x-api-key", "test"); // Backend şifresi
                conn.setDoOutput(true); // Veri göndereceğiz

                // 3. JSON Mesajı Oluşturma: {"message": "merhaba"}
                // Tırnak işaretlerini kaçırmak için basit bir replace işlemi yapıyoruz
                String jsonInputString = String.format("{\"message\": \"%s\"}", q.replace("\"", "\\\""));

                // 4. İsteği Gönderme
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // 5. Cevabı Okuma
                // Başarılı (200) değilse hata akışını okumak gerekir
                int code = conn.getResponseCode();
                InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // 6. JSON Parse Etme
                if (code == 200) {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String replyText = jsonResponse.optString("reply", "Cevap anlaşılamadı");
                    String audioB64 = jsonResponse.optString("audio", "");

                    // Audio varsa onu çal, yoksa standart TTS kullan
                    if (!audioB64.isEmpty()) {
                        playAudio(audioB64);
                    } else {
                        speak(replyText);
                    }
                } else {
                    speak("Sunucu hatası: " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
                speak("Bağlantı hatası oluştu");
            }
        }).start();
    }

    private void playAudio(String base64Sound) {
        try {
            // Ses verisini dosyaya yaz (Arka planda)
            byte[] decoded = Base64.decode(base64Sound, Base64.DEFAULT);
            File tempMp3 = File.createTempFile("niko_voice", ".mp3", getCacheDir());
            tempMp3.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(decoded);
            fos.close();

            // Medya oynatıcıyı UI thread'de başlat
            runOnUiThread(() -> {
                try {
                    MediaPlayer mp = new MediaPlayer();
                    mp.setDataSource(tempMp3.getAbsolutePath());
                    mp.prepare();
                    mp.start();

                    mp.setOnCompletionListener(mediaPlayer -> {
                        mediaPlayer.release();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            speak("Ses verisi işlenemedi.");
        }
    }

    // ================= TTS =================

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("tr", "TR"));

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Dil desteklenmiyorsa log basılabilir
                } else {
                    // TTS başarıyla yüklendiğinde kendini tanıt
                    speak("Merhaba, ben Niko. Emrinizdeyim.");
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) {
            }

            public void onDone(String id) {
                // Konuşma bitince mikrofona geçmek isterseniz burada startListening()
                // çağırabilirsiniz
            }

            public void onError(String id) {
            }
        });
    }

    private void speak(String t) {
        ttsQueue.offer(t);
        runOnUiThread(this::speakNext);
    }

    private void speakNext() {
        if (!tts.isSpeaking() && !ttsQueue.isEmpty()) {
            tts.speak(ttsQueue.poll(), TextToSpeech.QUEUE_FLUSH, null, "tts");
        }
    }

    // ================= WHATSAPP =================

    public static class WhatsAppService extends NotificationListenerService {

        @Override
        public void onNotificationPosted(StatusBarNotification sbn) {

            if (!"com.whatsapp".equals(sbn.getPackageName()))
                return;

            Notification n = sbn.getNotification();
            if (n == null)
                return;

            Bundle e = n.extras;

            lastWhatsAppMessage = String.valueOf(e.getCharSequence(Notification.EXTRA_TEXT));
            lastWhatsAppSender = String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE));

            if (n.actions != null) {
                for (Notification.Action a : n.actions) {
                    if (a.getRemoteInputs() != null) {
                        lastReplyIntent = a.actionIntent;
                        lastRemoteInput = a.getRemoteInputs()[0];
                    }
                }
            }
        }
    }

    private void readLastWhatsAppMessage() {
        if (lastWhatsAppMessage == null) {
            speak("Okunacak WhatsApp mesajı yok");
            return;
        }
        speak(lastWhatsAppSender + " şöyle yazmış: " + lastWhatsAppMessage);
    }

    private void replyWhatsApp(String msg) {

        if (!Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners")
                .contains(getPackageName())) {

            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return;
        }

        if (lastReplyIntent == null || lastRemoteInput == null)
            return;

        Intent i = new Intent();
        Bundle b = new Bundle();
        b.putCharSequence(lastRemoteInput.getResultKey(), msg);
        RemoteInput.addResultsToIntent(new RemoteInput[] { lastRemoteInput }, i, b);

        try {
            lastReplyIntent.send(this, 0, i);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null)
            speechRecognizer.destroy();
        if (tts != null)
            tts.shutdown();
    }
}
