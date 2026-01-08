#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <DHT.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <UniversalTelegramBot.h>
#include <ArduinoJson.h>
#include <WebServer.h>

// ==========================================
// --- 1. TES INFOS ---
// ==========================================
const char* ssid = "inwi Home 4G EB068E";     
const char* password = "46724211";   

// Infos Telegram
#define BOTtoken "8539600207:AAGiaLpJcMqNwGFezTt_sZgQJCXSN7yVx_A"  
#define CHAT_ID "1650880183"

// --- PINS ---
#define PIN_FLAMME      4
#define PIN_FUMEE       1
#define PIN_DHT         15
#define PIN_BUZZER      16
#define PIN_LED         17

// --- CONFIGURATION ---
#define DHTTYPE DHT11
DHT dht(PIN_DHT, DHTTYPE);
LiquidCrystal_I2C lcd(0x27, 16, 2); 
WebServer server(80); 

WiFiClientSecure client;
UniversalTelegramBot bot(BOTtoken, client);

// --- VARIABLES ---
int valeurFumee = 0;
float temperature = 0.0;
int etatFlamme = 0;
bool alarmeActive = false;
bool messageEnvoye = false; 

// Anti-Faux positifs Feu
unsigned long debutDetectionFeu = 0;
bool verificationFeuEnCours = false;

// Icones LCD
byte icoTermo[8]  = {B00100, B01010, B01010, B01110, B01110, B11111, B11111, B01110};
byte icoFlamme[8] = {B00100, B01110, B01110, B10101, B10101, B01110, B01110, B00100};
byte icoGaz[8]    = {B00000, B00000, B01110, B11111, B11101, B11111, B01110, B00000};
byte icoSon[8]    = {B00001, B00011, B00111, B11111, B11111, B00111, B00011, B00001};

// ==========================================
// --- DESIGN FUTURISTE DE LA PAGE WEB ---
// ==========================================
void handleRoot() {
  // Calculs couleurs
  String couleurStatus = alarmeActive ? "#ff003c" : "#00f3ff"; // Rouge Néon ou Cyan Néon
  String statusText = alarmeActive ? "DANGER CRITIQUE" : "SYSTÈME SÉCURISÉ";
  String shadowColor = alarmeActive ? "rgba(255, 0, 60, 0.7)" : "rgba(0, 243, 255, 0.5)";
  
  String feuTxt = (etatFlamme == HIGH) ? "<span style='color:#ff003c; text-shadow:0 0 10px red;'>DÉTECTÉ</span>" : "<span style='color:#00f3ff'>NÉGATIF</span>";
  String gazColor = (valeurFumee > 2500) ? "#ff003c" : "#00f3ff";
  String tempColor = (temperature > 45.0) ? "#ff003c" : "#00f3ff";

  server.setContentLength(CONTENT_LENGTH_UNKNOWN);
  server.send(200, "text/html", ""); 
  
  String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta http-equiv='refresh' content='3'>"; 
  html += "<meta name='viewport' content='width=device-width, initial-scale=1.0'>";
  // Import de la police futuriste 'Orbitron'
  html += "<link href='https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&display=swap' rel='stylesheet'>";
  
  html += "<style>";
  html += "body { font-family: 'Orbitron', sans-serif; background: linear-gradient(135deg, #0b0c15, #181a2e); color: #fff; text-align: center; margin: 0; padding: 20px; min-height: 100vh; }";
  html += ".container { max-width: 600px; margin: auto; }";
  
  // Style des Cartes (Glassmorphism)
  html += ".card { background: rgba(255, 255, 255, 0.03); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.1); padding: 20px; margin: 15px 0; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); transition: transform 0.3s; }";
  html += ".card:hover { transform: translateY(-5px); border-color: " + couleurStatus + "; }";
  
  html += "h1 { font-size: 24px; letter-spacing: 2px; color: #888; text-transform: uppercase; margin-bottom: 5px; }";
  html += ".status-box { padding: 20px; border-radius: 15px; border: 2px solid " + couleurStatus + "; box-shadow: 0 0 20px " + shadowColor + "; margin-bottom: 30px; animation: pulse 2s infinite; }";
  html += "h2 { margin: 0; font-size: 28px; color: " + couleurStatus + "; text-shadow: 0 0 15px " + shadowColor + "; }";
  
  html += ".value { font-size: 50px; font-weight: bold; margin: 10px 0; }";
  html += ".label { font-size: 14px; color: #aaa; letter-spacing: 1px; text-transform: uppercase; }";
  
  // Animation de pulsation
  html += "@keyframes pulse { 0% { box-shadow: 0 0 10px " + shadowColor + "; } 50% { box-shadow: 0 0 25px " + shadowColor + ", 0 0 10px " + couleurStatus + "; } 100% { box-shadow: 0 0 10px " + shadowColor + "; } }";
  html += "</style></head><body>";
  
  html += "<div class='container'>";
  html += "<h1>ESP32 Monitor v4.0</h1>";
  
  // Bloc Statut Principal
  html += "<div class='status-box'><h2>" + statusText + "</h2></div>";
  
  // Carte Temperature
  html += "<div class='card'><div class='label'>Température Ambiante</div>";
  html += "<div class='value' style='color:" + tempColor + "; text-shadow: 0 0 10px " + tempColor + ";'>" + String(temperature, 1) + "°C</div></div>";
  
  // Carte Gaz
  html += "<div class='card'><div class='label'>Qualité de l'Air (PPM)</div>";
  html += "<div class='value' style='color:" + gazColor + "; text-shadow: 0 0 10px " + gazColor + ";'>" + String(valeurFumee) + "</div></div>";
  
  // Carte Feu
  html += "<div class='card'><div class='label'>Capteur Infrarouge</div>";
  html += "<div class='value'>" + feuTxt + "</div></div>";
  
  // IP footer
  html += "<div style='color: #444; font-size: 10px; margin-top: 30px;'>SYSTEM ID: " + WiFi.localIP().toString() + "</div>";
  html += "</div></body></html>";
  
  server.sendContent(html);
}

void setup() {
  Serial.begin(115200);

  // LCD & Pins
  Wire.begin(8, 9); 
  lcd.init(); lcd.backlight();
  lcd.createChar(0, icoTermo); lcd.createChar(1, icoFlamme); 
  lcd.createChar(2, icoGaz); lcd.createChar(3, icoSon);    
  pinMode(PIN_FLAMME, INPUT); pinMode(PIN_FUMEE, INPUT);
  pinMode(PIN_BUZZER, OUTPUT); pinMode(PIN_LED, OUTPUT);
  dht.begin();

  // WiFi
  lcd.setCursor(0, 0); lcd.print("Boot System...");
  Serial.print("Connexion au WiFi : "); Serial.println(ssid);
  
  WiFi.begin(ssid, password);
  int tentatives = 0;
  while (WiFi.status() != WL_CONNECTED && tentatives < 20) { 
    delay(500); 
    Serial.print("."); 
    tentatives++;
  }

  if(WiFi.status() == WL_CONNECTED) {
    Serial.println("\nONLINE.");
    Serial.print("IP : "); Serial.println(WiFi.localIP());
  } else {
    Serial.println("\nOFFLINE (Erreur WiFi).");
  }

  // Config Telegram
  client.setInsecure(); 

  // Affichage IP LCD
  lcd.clear();
  lcd.setCursor(0, 0); lcd.print("IP Adr:");
  lcd.setCursor(8, 0); lcd.print(WiFi.localIP());

  server.on("/", handleRoot);
  server.begin();
  
  // Test Telegram
  bot.sendMessage(CHAT_ID, "🚀 SYSTÈME ONLINE : Interface Futuriste Chargée.", "");
}

void loop() {
  server.handleClient(); 

  // --- LECTURE ---
  int lectureFlammeBrute = digitalRead(PIN_FLAMME); 
  valeurFumee = analogRead(PIN_FUMEE);  
  temperature = dht.readTemperature();
  if (isnan(temperature)) temperature = 0;

  // --- ANTI-FAUX POSITIFS FEU ---
  if (lectureFlammeBrute == HIGH) {
    if (!verificationFeuEnCours) {
      debutDetectionFeu = millis();
      verificationFeuEnCours = true;
    } else if (millis() - debutDetectionFeu > 500) {
      etatFlamme = HIGH; 
    }
  } else {
    verificationFeuEnCours = false;
    etatFlamme = LOW;
  }

  // --- ALARME ---
  alarmeActive = (etatFlamme == HIGH || valeurFumee > 2500 || temperature > 45.0);

  if (alarmeActive) {
    // 1. LCD
    lcd.setCursor(0, 1); lcd.print("! DANGER ! "); lcd.write(3);
    
    // 2. Telegram (Une seule fois)
    if (messageEnvoye == false) {
      String message = "🚨 ALERTE CRITIQUE ! 🚨\n";
      if(etatFlamme == HIGH) message += "🔥 FEU DÉTECTÉ !\n";
      if(valeurFumee > 2500) message += "☣️ GAZ TOXIQUE !\n";
      if(temperature > 45.0) message += "🌡️ SURCHAUFFE !\n";
      
      if(bot.sendMessage(CHAT_ID, message, "")) {
        messageEnvoye = true; 
      }
    }

    // 3. LE SON RYTHMÉ (BIP BIP)
    tone(PIN_BUZZER, 2500);     
    digitalWrite(PIN_LED, HIGH);
    delay(150);                 
    
    noTone(PIN_BUZZER);        
    digitalWrite(PIN_LED, LOW); 
    delay(150);                

  } else {
    // Mode Calme
    noTone(PIN_BUZZER);     
    digitalWrite(PIN_LED, LOW);
    messageEnvoye = false; 

    // Affichage LCD rotatif simple
    lcd.setCursor(0, 1);
    lcd.print("T:"); lcd.print((int)temperature); lcd.print("c ");
    lcd.print("G:"); lcd.print(valeurFumee); lcd.print("   ");
    
    delay(100);
  }
}