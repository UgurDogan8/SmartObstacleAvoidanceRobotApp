# Smart Robot Arduino Code

Bu klasör, "Smart Obstacle Avoidance Robot" projesinin Arduino Nano mikrodenetleyicisi üzerinde çalışan donanım kodlarını (`SmartRobot.ino`) içermektedir. Sistem, C++ ile yazılmış olup Android mobil uygulamasıyla HC-05 Bluetooth modülü üzerinden çift yönlü haberleşecek şekilde optimize edilmiştir.

## 🛠 Donanım Özellikleri ve Pin Bağlantıları

- **Mikrodenetleyici:** Arduino Nano
- **Bluetooth Modülü:** HC-05
- **Motor Sürücü:** L298N (Sağ motor hız: D5, Sol motor hız: D6, Yön pinleri: D8, D9, D10, D11)
- **Ultrasonik Sensör:** HC-SR04 (Trig: A2, Echo: D4)
- **Radar Servosu:** SG90 / MG996R (Sinyal: A0)
- **Pil Ölçüm Pini:** Gerilim bölücü devre üzerinden A1
- **Acil Durum Butonu:** D2 (Kesme / INT0)
- **Hız (Enkoder) Sensörü:** D3 (Kesme / INT1)

## 🚀 Yazılımın Temel Özellikleri

Kodlama mimarisinde stabilite ve güvenliği sağlamak için çeşitli algoritmalar ve kontroller kullanılmıştır:

### 1. Çift Çalışma Modu (Manuel ve Otonom)
- **Otonom Mod (Engelden Kaçma):** Robot önündeki engelleri algılar. 30 cm ve üzerinde tam hız ilerlerken, 15-30 cm arası hızını düşürür. 15 cm'den daha yakın bir engel tespit ederse durur, servo motor aracılığıyla sağa (50°) ve sola (140°) bakarak daha açık olan yöne doğru manevra yapar.
- **Korumalı Manuel Mod:** Kullanıcı kontrolündedir ancak ultrasonik sensör çalışmaya devam eder. Eğer aracın önüne 15 cm'den daha yakın bir engel çıkarsa araç ileri gitmeyi reddeder ve motorları durdurur (Çarpışma önleyici güvenlik).
- **Serbest Manuel Mod:** Sensör güvenlik kısıtlamaları devre dışı bırakılır, tam kontrol kullanıcıya verilir.

### 2. Gelişmiş Güvenlik ve Kesmeler (Interrupts)
- **Donanımsal Acil Durdurma:** D2 pinine bağlı fiziksel butona basıldığında veya mobil uygulamadan `E` komutu geldiğinde donanımsal kesme (Interrupt) tetiklenir ve araç anında kilitlenerek motorları durdurur. 
- **Watchdog Koruması:** Araç manuel sürüş modundayken, mobil cihazdan 1 saniyeden daha uzun süre hiçbir sinyal veya veri gelmezse (örneğin Bluetooth bağlantısı kopsa veya uygulama çökse), araç güvenlik amacıyla olduğu yerde durur.

### 3. Akıllı Takılma Algılayıcı (Unstuck Logic)
Otonom moddayken robot dar bir köşeye veya çukur bir engele sıkışırsa, bunu algılar. Mesafe 3 saniye boyunca değişmez ve çok yakın kalırsa, otomatik olarak "Kurtulma Manevrası" (Geri git ve sağa dön) başlatır.

### 4. Gerçek Zamanlı Telemetri ve Haritalama
Araç, saniyede bir kez (Timer2 kesmesi kullanılarak gecikmesiz olarak) Android uygulamasına kapsamlı bir telemetri paketi gönderir. 
Gönderilen veriler:
- **D:** Anlık mesafe
- **M & MS:** Güncel sürüş modu (Otonom, Korumalı/Serbest Manuel)
- **S & V:** Son komut ve hız PWM değeri
- **B:** Pil yüzdesi (Analog okuma ile gerilim ölçümü)
- **E:** Enkoder darbe sayısı (Katedilen yol)
- **MAP:** Radar ekranı ve haritalama için servo açısı, okunan mesafe ve darbe senkronizasyonu.

## 📥 Kurulum

1. Bilgisayarınıza [Arduino IDE](https://www.arduino.cc/en/software) indirin ve kurun.
2. `arduino/SmartRobot/SmartRobot.ino` dosyasını Arduino IDE ile açın.
3. Kütüphane yöneticisinden `Servo` kütüphanesinin yüklü olduğundan emin olun (Genellikle dahili gelir).
4. Arduino Nano'nuzu USB ile bağlayın ve doğru COM portunu seçerek kodu yükleyin. 

> [!WARNING]  
> Kodu yüklerken HC-05 Bluetooth modülünüzün RX ve TX pinlerinin Arduino'dan sökülü olduğundan emin olun, aksi halde kod yükleme sırasında hata alırsınız.
