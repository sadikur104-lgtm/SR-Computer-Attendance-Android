# অনলাইনে APK Build করার সহজ নিয়ম (Android Studio ছাড়াই)

এই ZIP-এ GitHub Actions workflow যোগ করা আছে। GitHub-এ প্রজেক্টের ফাইল আপলোড করলে GitHub-এর সার্ভার APK তৈরি করবে।

## ধাপ
1. GitHub.com-এ নিজের অ্যাকাউন্টে Sign in করুন।
2. **New repository** তৈরি করুন; নাম দিন `SR-Computer-Attendance-Android` এবং **Private** রাখুন।
3. ZIP Extract করে `AndroidProject` ফোল্ডারের ভেতরের সব ফাইল/ফোল্ডার repository-তে আপলোড করুন। `.github` ফোল্ডারটিও অবশ্যই আপলোড করবেন। `AndroidProject` নামের বাইরের ফোল্ডারটি নয়—ভেতরের `app`, `gradle` (থাকলে), `.github`, `build.gradle.kts`, `settings.gradle.kts` ইত্যাদি repository-র root-এ থাকবে।
4. আপলোড শেষে repository-র **Actions** ট্যাব খুলুন।
5. বাম পাশে **Build SR Computer Attendance APK** নির্বাচন করে **Run workflow** চাপুন। প্রথমবার workflow দেখা না গেলে `main` branch-এ একটি commit হয়েছে কি না দেখুন।
6. Build শেষ হলে সবুজ ✔ দেখাবে। সেই run-এ ঢুকে নিচের **Artifacts** অংশ থেকে `SR-Computer-Attendance-debug-apk` ডাউনলোড করুন।
7. ZIP extract করলে `app-debug.apk` পাবেন। ফোনে পাঠিয়ে install করুন। Android চাইলে ওই APK install করার অনুমতি দিন।

## নোট
- APK তৈরি করতে GitHub-এর অনলাইন build runner ব্যবহার হবে; PC-তে Android Studio লাগবে না।
- প্রথমবার build-এ কয়েক মিনিট লাগতে পারে।
- এটি debug APK; Play Store release/signed APK নয়।
- বর্তমান Android app প্রাথমিক সংস্করণ। PDF এখন সংক্ষিপ্ত summary; Excel export, backup/restore এবং পূর্ণ মাসিক PDF grid এখনো যোগ হয়নি।
- Windows অ্যাপের database/photos এই Android অ্যাপে স্বয়ংক্রিয়ভাবে কপি হবে না। প্রথম চালুতে নতুন ফাঁকা SQLite database হবে।
