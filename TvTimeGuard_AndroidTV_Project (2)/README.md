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