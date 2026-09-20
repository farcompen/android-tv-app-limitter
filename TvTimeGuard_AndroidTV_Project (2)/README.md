# 📺 TV TimeGuard - Android TV & Xiaomi TV Box Ebeveyn Kontrolü

## ⚡ Hızlı APK Alma (3 Yöntem)

### 🥇 Yöntem 1: GitHub Actions ile 1 Tıkla Bulutta APK Üretme (Önerilen)
1. Bu klasörü bir GitHub deposuna yükleyin (veya depoyu fork edin).
2. GitHub'da **Actions** sekmesine gidin.
3. **"Build Android TV APK"** iş akışını seçip **Run workflow** butonuna basın.
4. 1-2 dakika içinde **Artifacts** bölümünden doğrudan derlenmiş ve imzalanmış `TvTimeGuard_XiaomiTV_Release_APK` dosyasını indirin.

---

### 🥈 Yöntem 2: Android Studio ile APK Alma
1. **Android Studio**'yu açın -> **Open** diyerek bu klasörü seçin.
2. Gradle senkronizasyonu tamamlandığında üst menüden:
   `Build > Build Bundle(s) / APK(s) > Build APK(s)` seçeneğine tıklayın.
3. `app/build/outputs/apk/release/app-release.apk` dosyanız hazır!

---

### 🥉 Yöntem 3: Terminal / Komut Satırı
```bash
# Linux / macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```
Çıktı konumu: `app/build/outputs/apk/release/app-release.apk`

---

## 📺 Xiaomi TV / Mi Stick Kurulumu
1. APK dosyasını TV'ye gönderin (**Send Files to TV** uygulaması veya USB bellek ile).
2. TV'de dosya yöneticisiyle APK'yı kurun.
3. **Ayarlar > Erişilebilirlik > TV TimeGuard** servisini AÇIK yapın.
4. **Ayarlar > Özel Uygulama Erişimi > Kullanım Erişimi** iznini onaylayın.
5. Tamamen yerel SQLite veritabanı ile süre kontrolü başlayacaktır!
# Cagan TV TimeGuard v1.4.0

Android telefon, Android TV ve Google TV cihazlarında uygulama bazında günlük kullanım limiti uygular.

## v1.4.0 yenilikleri

- Telefonda ve Android TV'de kullanıcı tarafından açılabilen kurulu uygulama ve oyunlar otomatik taranır.
- Uygulama adı veya paket adına göre arama yapılabilir.
- Listeden istenen uygulamaya günlük süre limiti verilebilir veya limit kapatılabilir.
- Yeniden Tara düğmesi, sonradan kurulan uygulamaları listeye ekler.
- Uygulama listesi ve seçilen limitler yalnızca cihazda işlenir.

## v1.3.0 yenilikleri

- Android telefon desteği eklendi; TV özelliği artık zorunlu değil.
- Telefon ve TV launcher girişleri aynı uygulama paketinde çalışır.
- Telefon YouTube, Netflix, Prime Video, Spotify ve TikTok paketleri takip listesine eklendi.
- Dikey telefon ekranında ana sayfa kartları ve yönetim butonları alt alta yerleşir.
- Ayarlar ekranı PIN istemeden açılır.
- Ayarlar içine kurtarma koduyla unutulan PIN'i sıfırlama seçeneği eklendi.
- Kurtarma kodu ayarlar açıldığında gizlenir; görüntüleme/yenileme mevcut PIN gerektirir.
- Banner alt metni tüm cihazları kapsayacak şekilde “ANDROID” olarak güncellendi.

## v1.2.0 yenilikleri

- Uygulama açılışındaki PIN kaldırıldı; ana ekran ve istatistikler doğrudan açılır.
- Uygulama limitleri, güvenlik ayarları ve sistem izinleri ebeveyn PIN'i ile korunur.
- Erişilebilirlik ve kullanım erişimi açılmadan önce açık kullanım açıklaması gösterilir.
- Google Play için AAB üretimi ve güvenli upload-key yapılandırması eklendi.
- Play Store metinleri, gizlilik politikası, yayın kontrol listesi ve mağaza görselleri eklendi.
- Gereksiz özel izinler kaldırıldı; release sürümü artık debug anahtarıyla imzalanmaz.

## v1.1.0 yenilikleri

- Yönetim ekranı açılırken ebeveyn PIN'i sorulur (ilk kurulum varsayılanı: `1234`).
- Ayarlar ekranından eski PIN doğrulanarak 4-8 rakamlı yeni PIN belirlenebilir.
- İnternetsiz kurtarma için 8 rakamlı kurtarma kodu üretir. Kod güvenli bir yere kaydedilmelidir.
- PIN unutulduğunda kurtarma koduyla yeni PIN oluşturulabilir. Kod da kayıpsa Android uygulama verisini temizlemek gerekir; kullanım geçmişi silinir.
- TV kumandası odağındaki menü daha parlak arka plan ve çerçeveyle görünür.
- İstatistikler son 7 gün sütun grafiği ve bugünkü uygulama dağılım çubuklarıyla gösterilir.
- YouTube sayacı, Accessibility olaylarına ek olarak UsageStats üzerinden iki saniyede bir ön plan doğrulaması yapar.
- Sayaç ekran kapalıyken ilerlemez; eşzamanlı veritabanı yazımları engellenir.
- Limit ekranındaki doğru PIN, o günkü kullanımdan 30 dakika düşerek yalnızca o güne ek süre verir.

## Kurulum sonrası gerekli izinler

Ana ekrandaki İzinler bölümünden:

1. TV TimeGuard erişilebilirlik servisini etkinleştirin.
2. Kullanım erişimi iznini verin. YouTube sayaç kararlılığı için gereklidir.
3. Üstte gösterme iznini verin.

İlk girişten sonra varsayılan `1234` PIN'ini değiştirin ve Ayarlar ekranındaki kurtarma kodunu TV dışında saklayın.

## GitHub Actions ile APK

Repository'de `Actions > Build Android TV APK > Run workflow` yolunu kullanın. Başarılı çalışmanın altındaki `TvTimeGuard_XiaomiTV_Release_APK` artifact'i release APK'yı içerir.
