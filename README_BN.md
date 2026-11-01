# SR Computer Attendance — Android (Portrait)

এটি আপনার Windows V5 Attendance Sheet-এর আদলে তৈরি Offline Android সংস্করণের প্রাথমিক Native Kotlin প্রজেক্ট।

## বর্তমান প্রজেক্টে আছে
- Portrait মোবাইল UI, নীল-সাদা থিম
- Student add/edit এবং ছবি নির্বাচন
- Student soft-delete (তালিকা থেকে বাদ; attendance রেকর্ড ডাটাবেজে থাকে)
- মাস/বছর সামনে-পেছনে নেওয়া
- প্রতিদিনের ঘরে ট্যাপ করে P, A, Va, Ad, Mo, ENO, Off নির্বাচন/মুছে ফেলা
- তারিখের সারি horizontal swipe
- মাসিক এবং সব মাসের Overall totals
- সাধারণ PDF summary export
- SQLite local database; Firebase/Internet প্রয়োজন নেই
- প্রথমবার চালালে নতুন ফাঁকা ডাটাবেজ তৈরি হয়; ZIP-এর Windows attendance.db/photos আমদানি করা হয় না

## Build করে APK বানানো
1. Android Studio (সাম্প্রতিক stable) ইনস্টল করুন।
2. Android SDK Platform 35 এবং Android SDK Build-Tools ইনস্টল করুন।
3. Android Studio থেকে এই `AndroidProject` ফোল্ডার Open করুন।
4. Gradle sync শেষ হলে **Build > Build Bundle(s) / APK(s) > Build APK(s)** নির্বাচন করুন।
5. APK পাওয়া যাবে: `app/build/outputs/apk/debug/app-debug.apk`

প্রয়োজন: JDK 17 বা নতুন, Android Gradle Plugin 8.7.3, Kotlin 2.0.21।

## গুরুত্বপূর্ণ সীমাবদ্ধতা (প্রাথমিক সংস্করণ)
- এই পরিবেশে Android SDK/Gradle build tool ইনস্টল নেই, তাই এখানে APK compile বা device-test করা যায়নি।
- PDF export বর্তমানে সংক্ষিপ্ত attendance summary; PC-এর মতো পূর্ণ landscape মাসিক grid report নয়।
- Backup/Restore UI ও CSV/Excel export এখনো যোগ করা হয়নি।
- Student photo picker Android document URI ধরে রাখে; ফোনের Gallery/Files permission flow-এর উপর নির্ভর করে।
