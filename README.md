# SnapNews - Android News Application

SnapNews adalah aplikasi berita Android modern yang memberikan pengalaman membaca berita terkini dengan antarmuka yang elegan dan fitur-fitur canggih. Aplikasi ini dikembangkan sebagai bagian dari tugas final Lab Mobile 2025.

## 📱 Fitur Utama

### ✨ Fitur Wajib (Sesuai Requirement)
- **Dual Activity Architecture**: MainActivity (Launcher) dan DetailActivity
- **Intent Communication**: Navigasi antar Activity menggunakan Intent
- **RecyclerView**: Menampilkan daftar berita dengan smooth scrolling
- **Fragment Navigation**: Tiga fragment utama dengan Navigation Component
- **Background Threading**: Operasi database dan network di background thread
- **API Integration**: Menggunakan NewsAPI dengan Retrofit untuk data berita
- **Local Data Persistence**: SQLite database untuk caching dan favorit
- **Dark/Light Theme**: Toggle tema dengan SharedPreferences
- **Refresh on Error**: Tombol refresh saat koneksi gagal

### 🌟 Fitur Tambahan
- **Real-time Search**: Pencarian berita dengan debounce mechanism
- **Offline Reading**: Akses berita yang sudah di-cache tanpa koneksi
- **Favorites System**: Simpan dan kelola berita favorit
- **Share Functionality**: Bagikan artikel ke aplikasi lain
- **WebView Integration**: Baca artikel lengkap dalam aplikasi
- **Pull-to-Refresh**: Refresh dengan swipe gesture
- **Time Ago Display**: Tampilan waktu relatif (2 hours ago, etc.)
- **Image Caching**: Optimisasi loading gambar dengan Glide
- **Error Handling**: UI yang informatif untuk berbagai kondisi error

## 🏗️ Arsitektur Aplikasi

### Technical Stack
- **Language**: Java
- **Min SDK**: Android API 24 (Android 7.0)
- **Target SDK**: Android API 34
- **Architecture**: MVVM dengan Repository Pattern
- **Database**: Room (SQLite)
- **Network**: Retrofit2 + OkHttp3
- **Image Loading**: Glide
- **UI**: Material Design 3
- **Navigation**: Android Navigation Component

## 🔧 Setup & Installation

### Prerequisites
- Android Studio Arctic Fox atau lebih baru
- Android SDK API 24+
- Java Development Kit 8+
- Internet connection untuk build

### Installation Steps

1. **Clone Repository**
   ```bash
   git clone https://github.com/yourusername/snapnews.git
   cd snapnews
   ```

2. **Open in Android Studio**
   - Buka Android Studio
   - Select "Open an existing project"
   - Navigate ke folder snapnews

3. **Sync Project**
   - Tunggu Gradle sync selesai
   - Install missing SDK components jika diminta

4. **🔐 API Key Configuration (SECURE SETUP)**
   
   **IMPORTANT**: Jangan pernah commit API key ke repository!
   
   a. **Buat file `local.properties`** di root project (sejajar dengan `build.gradle`)
   ```properties
   # local.properties
   NEWS_API_KEY=87e66d5122824db289814d3efd2ae21b
   ```
   
   b. **Pastikan `local.properties` ada di `.gitignore`**
   ```
   # .gitignore
   local.properties
   ```
   
   c. **Untuk Tim Development**: Bagikan API key secara terpisah (email, chat, dll)
   
   d. **Verifikasi Setup**: Build project akan gagal jika API key tidak dikonfigurasi

5. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   # atau langsung run dari Android Studio
   ```

## 📖 Cara Penggunaan

### Navigation
- **Home Tab**: Berita terkini dari berbagai sumber
- **Search Tab**: Cari berita berdasarkan keyword
- **Favorites Tab**: Berita yang telah disimpan

### Fitur Interaksi
- **Tap artikel**: Buka detail lengkap
- **Pull down**: Refresh berita terbaru
- **Heart icon**: Tambah/hapus dari favorit
- **Share button**: Bagikan artikel
- **Browser button**: Buka di browser eksternal
- **Theme toggle**: Switch dark/light mode

### Offline Mode
- Aplikasi otomatis menyimpan berita yang sudah dimuat
- Dapat mengakses berita cached tanpa internet
- Favorit tersimpan permanen di local database

## 🎨 Design & UI/UX

### Material Design 3
- Modern card-based interface
- Consistent color scheme
- Responsive typography
- Smooth animations and transitions

### Responsive Design
- Optimized untuk berbagai ukuran layar
- Landscape/portrait orientation support
- Accessibility-friendly

### Dark Theme
- Automatic system theme detection
- Manual toggle dalam settings
- Consistent theming across all screens

## 🔌 API Integration

### NewsAPI Implementation
- **Base URL**: `https://newsapi.org/v2/`
- **Endpoints Used**:
  - `/top-headlines` - Berita utama
  - `/everything` - Pencarian berita
  - `/sources` - Sumber berita

### Request Examples
```java
// Top Headlines
GET /v2/top-headlines?country=us&apiKey=API_KEY

// Search News
GET /v2/everything?q=technology&sortBy=publishedAt&apiKey=API_KEY
```

### Error Handling
- Network timeouts dengan retry mechanism
- API rate limiting handling
- Graceful fallback ke cached data
- User-friendly error messages

## 💾 Database Schema

### Articles Table
```sql
CREATE TABLE articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    description TEXT,
    url TEXT,
    urlToImage TEXT,
    publishedAt TEXT,
    content TEXT,
    author TEXT,
    source TEXT,
    isFavorite INTEGER,
    timestamp INTEGER
);
```

### Database Operations
- **Insert**: Cache berita baru dari API
- **Update**: Toggle status favorit
- **Query**: Search offline, load favorites
- **Delete**: Cleanup old non-favorite articles

## 🧪 Testing

### Manual Testing Checklist
- [ ] App launches successfully
- [ ] News loading dari API
- [ ] Offline mode dengan cached data
- [ ] Search functionality
- [ ] Favorites add/remove
- [ ] Article detail view
- [ ] Share functionality
- [ ] Theme switching
- [ ] Pull-to-refresh
- [ ] Error state handling

### Performance Testing
- [ ] Smooth scrolling pada RecyclerView
- [ ] Image loading optimization
- [ ] Background thread operations
- [ ] Memory usage optimization

## 🚀 Build & Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### APK Location
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 🔐 Security & Privacy

### 🛡️ API Key Security (BEST PRACTICES)
- ✅ **API key disimpan di `local.properties`** (tidak di-commit ke Git)
- ✅ **Menggunakan BuildConfig** untuk akses API key yang aman
- ✅ **Automatic validation** untuk memastikan API key terkonfigurasi
- ✅ **Production-ready** security implementation

### Setup API Key Security:
1. **Buat `local.properties`**:
   ```properties
   NEWS_API_KEY=your_actual_api_key_here
   ```

2. **Pastikan `.gitignore` includes**:
   ```
   local.properties
   ```

3. **Untuk deployment**: Gunakan environment variables atau secure storage

### Data Protection
- No sensitive user data collection
- Local database tidak menyimpan data personal
- API communications over HTTPS

### Permissions
- `INTERNET`: Untuk API calls
- `ACCESS_NETWORK_STATE`: Check koneksi internet

## 📝 Credits & Attribution

### Developer
**Nama**: [Your Name]  
**NIM**: [Your Student ID]  
**Program**: Lab Mobile 2025  
**Tema**: Berita & Informasi  

### Third-Party Libraries
- **Retrofit2**: HTTP client untuk API calls
- **Glide**: Image loading dan caching
- **Room**: Local database ORM
- **Material Components**: UI components
- **Navigation Component**: Fragment navigation

### News API
- **Provider**: NewsAPI.org
- **Plan**: Free tier
- **Rate Limit**: 1000 requests/day

## 📄 License

Project ini dibuat untuk keperluan akademik Lab Mobile 2025. Penggunaan NewsAPI tunduk pada [Terms of Service NewsAPI](https://newsapi.org/terms).

## 🐛 Known Issues & Future Improvements

### Current Limitations
- No user authentication system
- Limited offline search capabilities
- No push notifications

### Future Enhancements
- [ ] Push notifications untuk breaking news
- [ ] User preferences dan personalisasi
- [ ] Social sharing dengan gambar
- [ ] Bookmark categories
- [ ] Widget untuk home screen
- [ ] Voice search functionality
- [ ] Multiple language support

## 📞 Support

Untuk pertanyaan atau dukungan teknis:
- **Email**: [your.email@student.ac.id]
- **GitHub Issues**: [Repository Issues Page]

---

**SnapNews** - Stay Informed, Stay Connected 📰✨
