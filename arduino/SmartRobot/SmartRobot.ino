#include <Servo.h>

// Servo Nesnesi
Servo radarServosu;

// HC-SR04 Sensör Pin Tanımlamaları
const int trigPin = A2;
const int echoPin = 4;

// L298N Pin Tanımlamaları
const int ENA = 5;   // Sağ motor hız -> D5
const int IN1 = 8;   // Sağ motor yön 1 -> D8
const int IN2 = 9;   // Sağ motor yön 2 -> D9
const int IN3 = 10;  // Sol motor yön 1 -> D10
const int IN4 = 11;  // Sol motor yön 2 -> D11
const int ENB = 6;   // Sol motor hız -> D6

// Pil Ölçüm Pini
const int PIL_PIN = A1;

// Kesme Pinleri
const int BUTON_PIN = 2; // D2 / INT0 -> Acil durum butonu
const int HIZ_PIN = 3;   // D3 / INT1 -> H2010 hız sensörü

// Ana çalışma modları
enum Mod { OTONOM, MANUEL };
volatile Mod mevcutMod = OTONOM;

// Manuel alt modları
enum ManuelSurusModu { KORUMALI_MANUEL, SERBEST_MANUEL };
ManuelSurusModu manuelSurusModu = KORUMALI_MANUEL;

// Güvenlik ve Zamanlayıcı Bayrakları
volatile bool acilDurumIptal = false;
volatile bool timerSaniyeBayragi = false;

// Motor ve Sensör Değişkenleri
int guncelHiz = 0;
volatile unsigned long tekerlekDarbeSayisi = 0;
volatile int timer2Sayac = 0;

char bluetoothVerisi = 'S';

// Manuel hız değeri: 0-255 PWM
volatile int manuelHiz = 200;

// Hız seviyesi: 0-9 arası
int hizSeviyesiGuncel = 7;

// Son ölçülen mesafe
long sonMesafe = 999;

// Son servo / sensör bakış açısı
int sonServoAcisi = 95;

// Watchdog / Bağlantı Koruması
unsigned long sonSinyalZamani = 0;

// Takılma Algılayıcı / Unstuck Logic
int oncekiMesafe = 0;
unsigned long takilmaZamani = 0;

void setup() {
  Serial.begin(9600);

  radarServosu.attach(A0);

  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  pinMode(ENA, OUTPUT);
  pinMode(ENB, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);

  pinMode(BUTON_PIN, INPUT_PULLUP);
  pinMode(HIZ_PIN, INPUT);

  attachInterrupt(digitalPinToInterrupt(BUTON_PIN), acilDurdurmaISR, FALLING);
  attachInterrupt(digitalPinToInterrupt(HIZ_PIN), tekerlekDarbeISR, FALLING);

  // Timer2 Ayarı
  noInterrupts();

  TCCR2A = 0;
  TCCR2B = 0;
  TCNT2  = 0;

  OCR2A = 124;
  TCCR2A |= (1 << WGM21);
  TCCR2B |= (1 << CS22) | (1 << CS21) | (1 << CS20);
  TIMSK2 |= (1 << OCIE2A);

  interrupts();

  sonSinyalZamani = millis();

  servoAciYaz(95);
  guvenliGecikme(1000);
}

void loop() {
  // Acil durum kilidi
  // E komutu veya fiziksel buton sonrası sistem burada bekler.
  // C komutu gelirse kilit kaldırılır.
  // Acil durumdayken bile standart telemetri gönderilir.
  while (acilDurumIptal) {
    motorDur();
    checkBluetooth();

    if (timerSaniyeBayragi) {
      timerSaniyeBayragi = false;
      telemetriGonder();
    }

    delay(50);
  }

  // Watchdog Koruması
  // Sadece manuel modda çalışır.
  // 1 saniyeden uzun süre telefondan sinyal gelmezse araç güvenli şekilde durur.
  if (mevcutMod == MANUEL && millis() - sonSinyalZamani > 1000) {
    motorDur();
    bluetoothVerisi = 'S';
  }

  checkBluetooth();

  // Her 1 saniyede bir standart telemetri gönder
  if (timerSaniyeBayragi) {
    timerSaniyeBayragi = false;
    telemetriGonder();
  }

  if (mevcutMod == MANUEL) {
    robotManuelYonet(bluetoothVerisi);
  }
  else {
    servoAciYaz(95);
    guvenliGecikme(100);

    long onMesafe = mesafeOlculen();
    sonMesafe = onMesafe;

    // Ön yön haritalama noktası
    telemetriGonderAciMesafe(95, onMesafe);

    // Otonom Mod Takılma Kontrolü
    if (abs(onMesafe - oncekiMesafe) <= 1 && onMesafe < 10) {
      if (takilmaZamani == 0) {
        takilmaZamani = millis();
      }

      if (millis() - takilmaZamani > 3000) {
        kurtulmaManevrasi();
        takilmaZamani = 0;
      }
    }
    else {
      takilmaZamani = 0;
      oncekiMesafe = onMesafe;
    }

    if (onMesafe >= 30) {
      motorIleri(200);
    }
    else if (onMesafe >= 15 && onMesafe < 30) {
      motorIleri(100);
    }
    else {
      motorDur();
      guvenliGecikme(400);

      servoAciYaz(50);
      guvenliGecikme(500);
      long sagMesafe = mesafeOlculen();
      sonMesafe = sagMesafe;
      telemetriGonderAciMesafe(50, sagMesafe);

      servoAciYaz(140);
      guvenliGecikme(500);
      long solMesafe = mesafeOlculen();
      sonMesafe = solMesafe;
      telemetriGonderAciMesafe(140, solMesafe);

      servoAciYaz(95);
      guvenliGecikme(300);

      if (sagMesafe > solMesafe && sagMesafe > 15) {
        motorSagaDon(150);
        guvenliGecikme(400);
      }
      else if (solMesafe > sagMesafe && solMesafe > 15) {
        motorSolaDon(150);
        guvenliGecikme(400);
      }
      else {
        motorGeri(150);
        guvenliGecikme(500);
        motorSagaDon(150);
        guvenliGecikme(500);
      }

      motorDur();
    }
  }

  guvenliGecikme(50);
}

// Servo açısını hem fiziksel olarak değiştirir hem de son açıyı hafızada tutar
void servoAciYaz(int aci) {
  sonServoAcisi = aci;
  radarServosu.write(aci);
}

// Güvenli gecikme
void guvenliGecikme(unsigned long ms) {
  unsigned long baslangic = millis();

  while (millis() - baslangic < ms) {
    checkBluetooth();

    if (mevcutMod == MANUEL || acilDurumIptal) {
      return;
    }
  }
}

// Bluetooth veri okuma
void checkBluetooth() {
  if (Serial.available() > 0) {
    // Herhangi bir veri geldiğinde watchdog zamanlayıcısını sıfırla
    sonSinyalZamani = millis();

    char gelenVeri = Serial.read();

    // Satır sonu karakterlerini ve ping komutunu hareket komutu olarak işleme
    if (gelenVeri == '\n' || gelenVeri == '\r' || gelenVeri == 'P') {
      return;
    }

    // Korumalı manuel mod
    if (gelenVeri == 'M') {
      mevcutMod = MANUEL;
      manuelSurusModu = KORUMALI_MANUEL;
      motorDur();
      bluetoothVerisi = 'S';
      Serial.println("MOD: KORUMALI MANUEL KONTROL AKTIF");
    }

    // Serbest manuel mod
    else if (gelenVeri == 'X') {
      mevcutMod = MANUEL;
      manuelSurusModu = SERBEST_MANUEL;
      motorDur();
      bluetoothVerisi = 'S';
      Serial.println("MOD: SERBEST MANUEL KONTROL AKTIF");
    }

    // Otonom mod
    else if (gelenVeri == 'A') {
      mevcutMod = OTONOM;
      motorDur();
      bluetoothVerisi = 'S';
      Serial.println("MOD: OTONOM ENGELDEN KACMA AKTIF");
    }

    // Mobil acil durdurma
    else if (gelenVeri == 'E') {
      motorDur();
      bluetoothVerisi = 'S';
      acilDurumIptal = true;
      Serial.println("MOBIL ACIL DURDURMA AKTIF");
    }

    // Acil durum kilidini kaldır
    else if (gelenVeri == 'C') {
      acilDurumIptal = false;
      bluetoothVerisi = 'S';
      mevcutMod = MANUEL;
      manuelSurusModu = KORUMALI_MANUEL;
      motorDur();
      sonSinyalZamani = millis();

      Serial.println("ACIL DURUM KILIDI KALDIRILDI - KORUMALI MANUEL MOD AKTIF");
    }

    // Hız komutu: V0 - V9
    else if (gelenVeri == 'V') {
      delay(5);

      if (Serial.available() > 0) {
        char hizKarakteri = Serial.read();

        if (hizKarakteri >= '0' && hizKarakteri <= '9') {
          int hizSeviyesi = hizKarakteri - '0';
          hizSeviyesiGuncel = hizSeviyesi;

          // 0-9 hız seviyesini 0-255 PWM bandına çevir
          manuelHiz = map(hizSeviyesi, 0, 9, 0, 255);

          // V0 durdurma olarak kalır.
          // V1-V9 arasında motorun kalkış yapabilmesi için minimum PWM 80 yapılır.
          if (hizSeviyesi > 0 && manuelHiz < 80) {
            manuelHiz = 80;
          }

          Serial.print("HIZ SEVIYESI: V");
          Serial.print(hizSeviyesi);
          Serial.print(" | PWM: ");
          Serial.println(manuelHiz);
        }
      }
    }

    // Sürüş komutları
    else {
      bluetoothVerisi = gelenVeri;
    }
  }
}

// Standart telemetri gönderimi
// Format:
// D=28;M=A0;S=F;V=5;B=75;E=120;EM=0;MS=K;ANG=95;MAP=95,28,120
//
// D   = Mesafe
// M   = Ana mod: A0 manuel, A1 otonom
// S   = Son sürüş komutu
// V   = Hız seviyesi
// B   = Batarya yüzdesi
// E   = Enkoder darbe sayısı
// EM  = Acil durum: 0/1
// MS  = Manuel alt mod: K korumalı, S serbest
// ANG = Servo / sensör bakış açısı
// MAP = Haritalama paketi: açı,mesafe,enkoder
void telemetriGonder() {
  int pilYuzdesi = pilYuzdesiOlc();

  // Manuel modda da radar widget ve harita güncel kalsın diye mesafe ölçülür
  if (mevcutMod == MANUEL) {
    sonMesafe = mesafeOlculen();
  }

  telemetriGonderAciMesafe(sonServoAcisi, sonMesafe);
}

// Belirli açı ve mesafe ile tam telemetri gönderir.
// Android tarafı haritalama için özellikle MAP=aci,mesafe,enkoder alanını kullanır.
void telemetriGonderAciMesafe(int aci, long mesafe) {
  int pilYuzdesi = pilYuzdesiOlc();

  Serial.print("D=");
  Serial.print(mesafe);

  Serial.print(";M=");
  if (mevcutMod == MANUEL) {
    Serial.print("A0");
  }
  else {
    Serial.print("A1");
  }

  Serial.print(";S=");
  Serial.print(bluetoothVerisi);

  Serial.print(";V=");
  Serial.print(hizSeviyesiGuncel);

  Serial.print(";B=");
  Serial.print(pilYuzdesi);

  Serial.print(";E=");
  Serial.print(tekerlekDarbeSayisi);

  Serial.print(";EM=");
  if (acilDurumIptal) {
    Serial.print("1");
  }
  else {
    Serial.print("0");
  }

  Serial.print(";MS=");
  if (manuelSurusModu == KORUMALI_MANUEL) {
    Serial.print("K");
  }
  else {
    Serial.print("S");
  }

  Serial.print(";ANG=");
  Serial.print(aci);

  Serial.print(";MAP=");
  Serial.print(aci);
  Serial.print(",");
  Serial.print(mesafe);
  Serial.print(",");
  Serial.println(tekerlekDarbeSayisi);
}

// Timer2 ISR
ISR(TIMER2_COMPA_vect) {
  timer2Sayac++;

  if (timer2Sayac >= 125) {
    timerSaniyeBayragi = true;
    timer2Sayac = 0;
  }
}

// Acil durdurma kesmesi
void acilDurdurmaISR() {
  motorDur();
  bluetoothVerisi = 'S';
  acilDurumIptal = true;
}

// Hız sensörü kesmesi
void tekerlekDarbeISR() {
  tekerlekDarbeSayisi++;
}

// Manuel sürüş yönetimi
void robotManuelYonet(char komut) {
  int yavasHiz = manuelHiz * 0.4;

  // Korumalı manuel modda ileri yönlü hareketleri 15 cm altında engelle
  if (manuelSurusModu == KORUMALI_MANUEL) {
    if (komut == 'F' || komut == 'G' || komut == 'I') {
      long manuelMesafe = mesafeOlculen();
      sonMesafe = manuelMesafe;
      sonServoAcisi = 95;

      if (manuelMesafe < 15) {
        motorDur();
        bluetoothVerisi = 'S';
        return;
      }
    }
  }

  switch (komut) {
    case 'F':
      motorIleri(manuelHiz);
      break;

    case 'B':
      motorGeri(manuelHiz);
      break;

    case 'L':
      motorSolaDon(manuelHiz);
      break;

    case 'R':
      motorSagaDon(manuelHiz);
      break;

    case 'G':
      motorIleriSol(manuelHiz, yavasHiz);
      break;

    case 'I':
      motorIleriSag(yavasHiz, manuelHiz);
      break;

    case 'H':
      motorGeriSol(manuelHiz, yavasHiz);
      break;

    case 'J':
      motorGeriSag(yavasHiz, manuelHiz);
      break;

    case 'S':
      motorDur();
      break;
  }
}

// Pil yüzdesi ölçümü
int pilYuzdesiOlc() {
  int analogDeger = analogRead(PIL_PIN);

  float okunanVoltaj = (analogDeger / 1023.0) * 5.05;
  float gercekPilVoltaji = okunanVoltaj * 3.2;

  int yuzde = (gercekPilVoltaji - 6.4) / (8.4 - 6.4) * 100;

  if (yuzde > 100) yuzde = 100;
  if (yuzde < 0) yuzde = 0;

  return yuzde;
}

// Mesafe ölçümü
long mesafeOlculen() {
  long sure;

  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);

  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);

  digitalWrite(trigPin, LOW);

  sure = pulseIn(echoPin, HIGH, 30000);

  if (sure == 0) return 999;

  return sure * 0.034 / 2;
}

// Takılma anında kullanılacak otonom kurtulma manevrası
void kurtulmaManevrasi() {
  motorGeri(150);
  guvenliGecikme(500);

  motorSagaDon(150);
  guvenliGecikme(500);

  motorDur();
}

// İleri motor fonksiyonu
void motorIleri(int hedefHiz) {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  if (guncelHiz < hedefHiz) {
    for (int hiz = guncelHiz; hiz <= hedefHiz; hiz += 15) {
      analogWrite(ENA, hiz);
      analogWrite(ENB, hiz);

      checkBluetooth();

      if (acilDurumIptal) {
        return;
      }

      if (mevcutMod == MANUEL && bluetoothVerisi != 'F') {
        return;
      }

      delay(20);
    }
  }
  else {
    analogWrite(ENA, hedefHiz);
    analogWrite(ENB, hedefHiz);
  }

  guncelHiz = hedefHiz;
}

void motorGeri(int hiz) {
  analogWrite(ENA, hiz);
  analogWrite(ENB, hiz);

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  guncelHiz = 0;
}

void motorSagaDon(int hiz) {
  analogWrite(ENA, hiz);
  analogWrite(ENB, hiz);

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  guncelHiz = 0;
}

void motorSolaDon(int hiz) {
  analogWrite(ENA, hiz);
  analogWrite(ENB, hiz);

  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  guncelHiz = 0;
}

void motorDur() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);

  analogWrite(ENA, 0);
  analogWrite(ENB, 0);

  guncelHiz = 0;
}

// Çapraz yön motor fonksiyonları
void motorIleriSol(int sagHiz, int solHiz) {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  analogWrite(ENA, sagHiz);
  analogWrite(ENB, solHiz);

  guncelHiz = 0;
}

void motorIleriSag(int sagHiz, int solHiz) {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  analogWrite(ENA, sagHiz);
  analogWrite(ENB, solHiz);

  guncelHiz = 0;
}

void motorGeriSol(int sagHiz, int solHiz) {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  analogWrite(ENA, sagHiz);
  analogWrite(ENB, solHiz);

  guncelHiz = 0;
}

void motorGeriSag(int sagHiz, int solHiz) {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  analogWrite(ENA, sagHiz);
  analogWrite(ENB, solHiz);

  guncelHiz = 0;
}
