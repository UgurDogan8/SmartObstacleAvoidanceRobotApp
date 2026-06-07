# Akıllı Engelden Kaçan Robot Kontrol Uygulaması (Smart Obstacle Avoidance Robot)

## Proje Amacı
Bu projenin amacı, Arduino Nano mikrodenetleyicisi tabanlı otonom ve manuel olarak kontrol edilebilen bir mobil robot aracın, Android işletim sistemli bir mobil cihaz üzerinden HC-05 Bluetooth modülü aracılığıyla kablosuz olarak kontrol edilmesini ve izlenmesini sağlamaktır. Sistem, kullanıcının araca uzaktan manuel sürüş komutları göndermesine, otonom engelden kaçma modunu devreye almasına ve araçtan gelen anlık telemetri (engel mesafesi vb.) verilerini görüntülemesine olanak tanır.

## Kullanılan Donanımlar
- **Mikrodenetleyici:** Arduino Nano
- **Bluetooth Modülü:** HC-05 Bluetooth 2.0 Modülü
- **Motor Sürücü:** L298N (veya L293D / TB6612FNG gibi alternatif motor sürücüler)
- **Motorlar:** DC Redüktörlü Motorlar (2 veya 4 tekerlekten çekişli sisteme uygun)
- **Sensör:** HC-SR04 Ultrasonik Mesafe Sensörü
- **Güç Kaynağı:** Li-Po batarya (7.4V - 11.1V) veya uygun voltajlı pil/batarya grubu (18650 vb.)
- **Şasi:** Mobil robot araba şasisi

## Mobil Uygulama Özellikleri
- **Bluetooth Bağlantı Yönetimi:** Eşleştirilmiş cihazları listeleme, HC-05 modülüne bağlanma ve bağlantıyı kesme.
- **Manuel Kontrol:** İleri, Geri, Sağ ve Sol yön komutları ile aracı manuel olarak yönlendirme.
- **Mod Seçimi:** Manuel sürüş modu ile Otonom (Engelden Kaçma) sürüş modu arasında tek dokunuşla geçiş yapabilme.
- **Hız Kontrolü:** Uygulama üzerinden motor hız seviyesini ayarlayabilme.
- **Acil Durdurma:** Tek bir buton ile aracı anında durdurabilme.
- **Gerçek Zamanlı Telemetri İzleme:** Robot üzerindeki sensörlerden gelen verileri (örneğin, ultrasonik sensörden okunan engel mesafesi) anlık olarak ekranda görüntüleme.

## Bluetooth Komut Protokolü
Mobil uygulamadan robot üzerindeki Arduino'ya gönderilen komutlar karakter veya string tabanlıdır. İletişim, sistemin hızlı tepki verebilmesi için optimize edilmiştir.
Aşağıdaki komutlar sistemde kullanılan örnek protokolü temsil eder:
- `F`: İleri (Forward)
- `B`: Geri (Backward)
- `L`: Sola Dön (Left)
- `R`: Sağa Dön (Right)
- `S`: Dur (Stop / Acil Durdurma)
- `A`: Otonom Modu Aç (Auto/Autonomous Mode)
- `M`: Manuel Modu Aç (Manual Mode)
- `V<değer>`: Hız Ayarı (Örn: `V50` - %50 hız, `V100` - Maksimum hız)

## Telemetri Formatı
Robottan mobil uygulamaya gönderilen veriler ayrıştırılabilmesi için belirli bir formatta iletilir.
Örnek Telemetri Formatı:
`D:15\n`
- `D`: Distance (Mesafe anlamına gelen anahtar karakter)
- `15`: Sensörden okunan anlık mesafe değeri (santimetre cinsinden)
- `\n`: Satır sonu karakteri (Mobil uygulamanın verinin bittiğini anlaması ve ayrıştırması için kullanılır)

## Android İzinleri
Uygulamanın Bluetooth donanımına erişebilmesi ve iletişim kurabilmesi için `AndroidManifest.xml` dosyasında aşağıdaki izinler tanımlanmıştır:
- `android.permission.BLUETOOTH`: Bluetooth adaptörüne bağlanmak ve veri alışverişi yapmak için temel izin.
- `android.permission.BLUETOOTH_ADMIN`: Cihazları keşfetmek ve Bluetooth ayarlarını değiştirmek için.
- `android.permission.BLUETOOTH_CONNECT` (Android 12 ve üzeri): Eşleştirilmiş Bluetooth cihazlarıyla iletişim kurmak için çalışma zamanı (runtime) izni.
- `android.permission.BLUETOOTH_SCAN` (Android 12 ve üzeri): Çevredeki Bluetooth cihazlarını taramak için çalışma zamanı (runtime) izni.

## Kurulum Adımları
1. **Gereksinimler:** Bilgisayarınıza Android Studio'nun güncel sürümünü kurun.
2. **Projeyi İndirme:** Bu projeyi Git üzerinden bilgisayarınıza klonlayın veya arşiv dosyası (`.zip`) olarak indirip klasöre çıkartın.
3. **Projeyi Açma:** Android Studio'yu başlatın ve "Open an existing project" seçeneği ile projenin bulunduğu dizini seçin.
4. **Gradle Senkronizasyonu:** Proje açıldığında arka planda Gradle bağımlılıklarının indirilmesini ve senkronizasyon sürecinin tamamlanmasını bekleyin.
5. **Derleme:** Üst menüden `Build > Make Project` adımlarını izleyerek projeyi derleyin ve hata olmadığından emin olun.
6. **Cihaza Yükleme:** Bir Android cihazı bilgisayarınıza USB ile bağlayın (Cihazda "Geliştirici Seçenekleri" ve "USB Hata Ayıklama" açık olmalıdır) veya Android Studio üzerinden bir sanal cihaz (Emülatör) başlatın. Sonrasında "Run" (`Shift + F10`) butonuna tıklayarak uygulamayı cihaza yükleyin.

## HC-05 Eşleştirme Adımları
1. Robotun güç şalterini açarak sisteme enerji verin. HC-05 modülünün üzerindeki LED'in hızlıca yanıp söndüğünden emin olun (Bu, modülün eşleşmeye hazır olduğunu gösterir).
2. Android cihazınızın **Ayarlar > Bluetooth** menüsüne gidin ve Bluetooth'u aktif hale getirin.
3. Yeni cihazları tarayın ve listeden `HC-05` cihazını seçin.
4. Eşleştirme şifresi (PIN) istendiğinde genellikle `1234` veya `0000` girerek eşleştirmeyi tamamlayın. Eşleşme sonrası modül üzerindeki LED'in yanıp sönme frekansı yavaşlayacaktır.
5. Mobil uygulamayı başlatın, ekrandaki "Bağlan" (Connect) butonuna basın ve açılan listeden eşleştirdiğiniz HC-05 cihazını seçerek veri iletişimini başlatın.

## Uygulama Ekranları
1. **Bağlantı ve Cihaz Seçim Ekranı:** Kullanıcının daha önceden eşleştirilmiş Bluetooth cihazlarını listeleyip, robota (HC-05'e) bağlandığı arayüz.
2. **Ana Kontrol Paneli:**
   - **Yönlendirme Pad'i / Butonları:** İleri, Geri, Sağ, Sol ve Dur komutlarının gönderildiği buton grubu.
   - **Sürüş Modu Seçici:** Manuel veya Otonom sürüş modları arasında geçiş yapılmasını sağlayan anahtar (Switch) veya buton yapısı.
   - **Hız Kontrol Aracı:** Motor hızını dinamik olarak ayarlamak için kullanılan kaydırıcı (Slider / SeekBar).
   - **Telemetri Göstergesi:** Ultrasonik sensörden gelen mesafe verisinin (Örn: "Engel Mesafesi: 25 cm") anlık olarak ekrana yazdırıldığı metin alanı.
   - **Durum Çubuğu:** Bağlantı durumunu (Bağlanıyor, Bağlı, Bağlantı Koptu) gösteren bildirim alanı.

## Test Planı
Projenin doğrulama süreçleri aşağıdaki adımları kapsar:
1. **Bağlantı ve Kararlılık Testi:** Mobil uygulamanın HC-05 modülü ile sorunsuz bir şekilde bağlanabildiğinin, iletişim esnasında paket kaybı yaşanmadığının ve bağlantı koptuğunda sistemin (robotun) güvenli duruma (motorları durdurma) geçtiğinin test edilmesi.
2. **Manuel Sürüş Testi:** Kontrol paneli üzerinden gönderilen tüm yön komutlarının, robot üzerindeki motorlara doğru şekilde (doğru motorların doğru yöne dönmesiyle) yansıdığının doğrulanması.
3. **Hız Kontrol Testi:** Uygulama üzerinden gönderilen hız `Vxx` komutlarının, Arduino tarafından doğru PWM sinyallerine çevrilip motor devrini değiştirdiğinin gözlemlenmesi.
4. **Otonom Mod Fonksiyon Testi:** Otonom moda geçildiğinde robotun kendi başına hareket etmesi, ultrasonik sensör ile engelleri doğru mesafede tespit etmesi ve algoritmaya uygun manevralarla (sağa/sola dönerek) engelden kaçınması.
5. **Telemetri Gecikme Testi:** Ultrasonik sensör önüne engel konduğunda, ölçülen mesafenin mobil uygulamada kabul edilebilir bir gecikme (latency) süresi içerisinde güncellenmesinin teyit edilmesi.

## Bilinen Riskler
- **Bağlantı Kopması:** Bluetooth teknolojisinin menzil sınırlamaları nedeniyle, araç mobil cihazdan uzaklaştığında veya araya fiziksel engeller girdiğinde bağlantı kopabilir.
- **İletişim Gecikmesi (Latency):** Veri paketlerinin işlenmesi sırasında yaşanabilecek küçük gecikmeler, aracın yüksek hızlarda engellere çarpmasına neden olabilir. Otonom mod tepki süresi kritik bir faktördür.
- **Güç Tüketimi:** Motorların ani akım çekmesi veya bataryanın zayıflaması durumunda, HC-05 modülünün ve Arduino'nun besleme geriliminde çökmeler yaşanabilir, bu durum sistemsel kilitlenmelere (resetlenme) yol açabilir.
- **İzin ve Uyumluluk Sorunları:** Geliştirilen Android uygulamasının, farklı üreticilerin cihazlarında veya çok eski/yeni Android sürümlerinde Bluetooth izin politikaları sebebiyle beklenmedik şekilde çalışmayı durdurması ihtimali.

## Geliştirme Ortamı
- **IDE:** Android Studio
- **Programlama Dili (Mobil):** Kotlin / Java
- **Programlama Dili (Robot):** C++ (Arduino IDE)
- **Minimum SDK (Mobil):** API 24 (Android 7.0)
- **Hedef SDK (Mobil):** API 34 (Android 14)

## Geliştiriciler

- **Uğur DOĞAN:** Mobil uygulama geliştirmesi.
- **Ömer Arda BÜYÜKASLAN:** Gömülü yazılım ve donanım tasarımı.
## Entegrasyon Testi (Test Checklist)

Aşağıdaki adımları uygulayarak uygulamanın ve donanımın doğru entegre olduğunu doğrulayabilirsiniz. Testleri yapmadan önce donanımınızın açık ve HC-05 modülünün güce bağlı olduğundan emin olun.

| Adım | Test Senaryosu / Komut | Beklenen Sonuç (Uygulama / Donanım) |
|---|---|---|
| 1 | **HC-05 Eşleşme** | Uygulama açıldığında izinler istenmeli ve Bluetooth cihaz listesinde `HC-05` (veya modül adı) görünmelidir. Cihaz eşleşmemişse sistem ayarlarına yönlendirmelidir. |
| 2 | **Bluetooth Bağlantı** | `Bağlan` butonuna basıldığında uygulama `ControlScreen` (Kontrol Ekranı) arayüzüne geçmelidir. Ekranda "Bağlantı: Bağlı" yazmalı ve cihaz adı görünmelidir. |
| 3 | **`S` Komutu (Bağlantı Anı)** | Bağlantı kurulduğu an arka planda `S` (Dur) komutu gönderilmiş olmalı ve araç motorları kesinlikle hareket etmemelidir. |
| 4 | **`V5` Hız Komutu** | Bağlantı sonrası otomatik olarak varsayılan `V5` hız komutu araca gönderilmeli, arayüzde "Hız: 5/9" yazmalıdır. |
| 5 | **`A0` Manuel Mod** | Arayüzden "Manuel" butonuna basıldığında (veya bağlantı anında) `M=A0` komutu iletilmeli ve araç komut bekleme (Manuel) durumuna geçmelidir. |
| 6 | **`F` İleri Komutu** | İleri butonuna **basılı tutulduğunda**, araca `F` komutu gitmeli ve araç ileri doğru hareket etmelidir. Engel yoksa (yeşil) gitmeye devam etmelidir. |
| 7 | **`B` Geri Komutu** | Geri butonuna **basılı tutulduğunda**, araca `B` komutu gitmeli ve araç geriye gitmelidir. |
| 8 | **`L` Sol Komutu** | Sol butonuna basılı tutulduğunda, araca `L` komutu gitmeli ve araç olduğu yerde (veya hareket halinde) sola dönmelidir. |
| 9 | **`R` Sağ Komutu** | Sağ butonuna basılı tutulduğunda, araca `R` komutu gitmeli ve araç sağa dönmelidir. |
| 10 | **`S` Dur Komutu (Bırakma)** | Basılı tutulan **herhangi bir yön tuşu bırakıldığında**, otomatik olarak `S` komutu gönderilmeli ve araç anında durmalıdır. |
| 11 | **`A1` Otomatik Mod** | "Otomatik" butonuna basıldığında, araca `A1` komutu gitmeli, ekranda "Otonom engelden kaçma aktif" yazmalı ve araç kendi sensörleriyle bağımsız şekilde ilerlemelidir. (Bu sırada yön tuşları kilitlenmelidir). |
| 12 | **`E` Acil Durdurma** | Kırmızı "ACİL DURDUR" butonuna basıldığında, araca `E` komutu gitmeli, arayüz kırmızı "ACİL DURUM AKTİF" ekranıyla kilitlenmeli ve motorlar anında durmalıdır. "Sistemi Sıfırla" denilene kadar araç hiçbir komut almamalıdır. |
| 13 | **Telemetri Okuma (D=xx;M=xx)** | Araç çalıştığı sürece mesafe değiştiğinde (Örn: Elinizi sensöre yaklaştırdığınızda), arayüzdeki "Güvenli/Dikkat/Kritik" durumları değişmeli; 20cm altına inildiğinde ekran kızarmalı ve titreşim vermelidir. |
| 14 | **Bağlantı Kopması Testi** | Araç çalışırken HC-05'in gücü kesildiğinde veya uzaklaşıldığında, en geç 1 saniye içinde (Watchdog) ekranda "BAĞLANTI KOPTU" uyarısı çıkmalı ve araç güvenlik amacıyla durmalıdır (veya kontrol tuşları kilitlenmelidir). |

### Hata Ayıklama (Debugging)
Herhangi bir adımda beklenen sonucu alamazsanız, arayüzdeki **"Tanılama" (Diagnostics)** butonuna basarak anlık giden (Mavi `[TX]`) ve gelen (Yeşil `[RX]`) komut trafiğini inceleyebilirsiniz.
