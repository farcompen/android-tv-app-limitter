# Google Play ve Android TV yayın kontrol listesi

## Projede tamamlananlar

- Sürüm: 1.4.0 (`versionCode 5`)
- Android telefon ve Android TV ortak paket desteği
- Telefon ve TV ortak paket hedef API: 36
- Minimum API: 26
- Android App Bundle (`bundleRelease`) üretimi workflow'a eklendi
- Debug anahtarıyla release imzalama kaldırıldı
- İsteğe bağlı GitHub Secrets tabanlı upload key desteği eklendi
- 320×180 uygulama içi TV launcher banner'ı
- 160×160 xhdpi TV launcher ikonu
- 512×512 Play Store ikonu
- 1024×500 özellik grafiği
- 1280×720 Play Store Android TV banner'ı
- Açılış PIN'i kaldırıldı; ebeveyn işlemleri PIN ile korundu
- Erişilebilirlik ve kullanım erişimi için açık rıza/açıklama pencereleri eklendi
- Gereksiz üstte gösterme ve foreground-service izinleri kaldırıldı
- Uygulama yedeklemesi kapatıldı

## Yayınlamadan önce kullanıcı tarafından tamamlanacaklar

1. Play Console geliştirici hesabı oluşturun ve kimlik doğrulamasını tamamlayın.
2. Kalıcı bir upload keystore oluşturun ve güvenli yedekleyin. Bu anahtarı kaybetmeyin.
3. GitHub Secrets'a şunları ekleyin:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
4. `PRIVACY_POLICY_TR.md` içindeki e-posta yer tutucusunu gerçek geliştirici adresiyle değiştirin.
5. Gizlilik politikasını herkese açık HTTPS adresinde yayınlayın ve Play Console'a URL'yi girin.
6. Gerçek Android TV cihazından tercihen 4 adet 1920×1080 PNG/JPEG ekran görüntüsü alın:
   - Ana kontrol paneli
   - Taranan uygulamalar, arama ve limitler
   - Grafiksel istatistikler
   - Süre doldu/kilit ekranı
7. Android telefondan tercihen 4 adet yüksek çözünürlüklü dikey ekran görüntüsü alın: ana ekran, uygulamalar, istatistikler ve kilit ekranı.
8. Play Console'da App content bölümlerini doldurun: Veri güvenliği, hedef kitle, içerik derecelendirmesi, reklam beyanı ve uygulama erişimi.
9. Accessibility API kullanım beyanında temel işlevi açıkça anlatın; istenirse kısa kullanım videosu sağlayın.
10. Önce Internal testing kanalına imzalı AAB yükleyin ve gerçek telefon/TV cihazlarında test edin.
11. Setup > Advanced settings > Form factors bölümünden Android TV'yi etkinleştirin.

## Veri güvenliği için taslak cevap

Mevcut kodda internet izni ve harici sunucu aktarımı bulunmadığından kullanım verileri yalnızca cihaz üzerinde işlenir. Play Console formundaki cevaplar, yayın öncesinde eklenen SDK veya servisler varsa yeniden değerlendirilmelidir.

## GitHub için keystore Base64 oluşturma

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Content keystore-base64.txt
```

`keystore-base64.txt` içeriğini `ANDROID_KEYSTORE_BASE64` secret'ına ekleyin. Keystore veya secret değerlerini repoya yüklemeyin ve sohbet içinde paylaşmayın.
