# TriTunnel 🛡️

V2Ray + SSH + OpenVPN — তিন protocol সাপোর্ট করার লক্ষ্যে বানানো একটি Android tunnel/VPN অ্যাপ।
**PC ছাড়াই** GitHub Actions দিয়ে ক্লাউডে APK তৈরি হয়।

> ⚠️ এটি একটি চলমান প্রজেক্ট। ধাপে ধাপে তিনটি protocol যোগ হচ্ছে।

---

## 📦 এখন কী কাজ করে (Milestone 1)

| Protocol | অবস্থা |
|----------|--------|
| **SSH**  | ✅ কানেক্ট হয় + লোকাল SOCKS5 proxy খোলে |
| **V2Ray**| 🔜 পরের ধাপে (Xray core যোগ হবে) |
| **OpenVPN** | 🔜 পরের ধাপে (ics-openvpn core যোগ হবে) |
| পুরো-ফোন রাউটিং (VpnService + tun2socks) | 🔜 পরের ধাপে |

এখন অ্যাপ খুলে SSH সার্ভার যোগ করে কানেক্ট করলে একটা SOCKS5 proxy (127.0.0.1:10808) চালু হয়।

---

## 📲 APK কীভাবে পাবেন (PC লাগবে না)

1. এই repo-তে কোড push হলে **Actions** ট্যাবে গিয়ে দেখুন একটা build চলছে।
2. Build সবুজ (✓) হলে সেটায় ঢুকুন → নিচে **Artifacts** → **TriTunnel-debug-apk** ডাউনলোড করুন।
3. ফোনে ZIP খুলে ভিতরের `app-debug.apk` ইনস্টল করুন
   (Settings → "Unknown sources" অনুমতি দিতে হতে পারে)।

---

## 🧱 প্রজেক্ট কাঠামো

```
app/src/main/java/com/tritunnel/app/
├── MainActivity.kt          # অ্যাপ চালু হওয়ার জায়গা
├── data/
│   ├── ServerConfig.kt      # সার্ভারের তথ্য model
│   └── ConfigStore.kt       # সার্ভার সেভ/লোড
├── core/
│   ├── Tunnel.kt            # সব protocol-এর common interface
│   ├── TunnelManager.kt     # কানেকশন অবস্থা নিয়ন্ত্রণ
│   ├── ssh/SshTunnel.kt     # ✅ SSH (কাজ করে)
│   ├── v2ray/V2RayTunnel.kt # 🔜 V2Ray (TODO)
│   └── openvpn/OpenVpnTunnel.kt # 🔜 OpenVPN (TODO)
├── service/AppVpnService.kt # 🔜 VpnService (TODO)
└── ui/MainScreen.kt         # পুরো UI (Jetpack Compose)
```

---

## 🛠️ পরের ধাপগুলো (roadmap)

1. **VpnService + tun2socks** — পুরো ফোনের ট্রাফিক tunnel দিয়ে পাঠানো।
2. **V2Ray core** — `app/libs/libv2ray.aar` যোগ করে `V2RayTunnel` সম্পূর্ণ করা।
3. **OpenVPN core** — `ics-openvpn` module যোগ করে `OpenVpnTunnel` সম্পূর্ণ করা।
4. সার্ভার এডিট, import/export, auto-reconnect ইত্যাদি।

---

## 💻 চাইলে নিজের PC-তে build (ঐচ্ছিক)

```bash
# Android Studio দিয়ে খুলুন, অথবা:
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

প্রয়োজন: JDK 17, Android SDK (compileSdk 34)।

---

## ⚖️ লাইসেন্স ও দায়বদ্ধতা

শুধুমাত্র শিক্ষা ও বৈধ ব্যবহারের জন্য। নিজের দায়িত্বে ব্যবহার করুন।
