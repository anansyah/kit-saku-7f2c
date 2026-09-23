#!/usr/bin/env bash
# Perakit paket Android memakai perkakas SDK yang sudah ada di mesin.
set -euo pipefail

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
[ -d "$SDK" ] || { echo "SDK tidak ditemukan"; ls -d /usr/local/lib/android/* 2>/dev/null; exit 1; }

PLAT=$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sort -V | tail -1)
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
[ -n "$PLAT" ] && [ -n "$BT" ] || { echo "platform/build-tools kosong"; ls "$SDK"; exit 1; }

# pakai platform STABIL (bukan beta/pratinjau) supaya bisa dipasang di HP umum
STABIL=$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sed 's#.*/android-##' | grep -E '^[0-9]+$' | sort -n | tail -1)
if [ -n "$STABIL" ] && [ -f "$SDK/platforms/android-$STABIL/android.jar" ]; then
  PLAT="$SDK/platforms/android-$STABIL"
fi

JAR="$PLAT/android.jar"
API=$(basename "$PLAT" | sed 's/android-//')
TARGET="${TARGET_API:-$API}"
echo "SDK      : $SDK"
echo "platform : $PLAT (API $API)"
echo "target   : API $TARGET (stabil)"
echo "build    : $BT"

# Periksa dulu: kesalahan escape/lambda/kurung menggagalkan javac jauh di ujung.
# Lebih baik ketahuan di detik pertama daripada menunggu perakitan selesai.
if [ -f tools/periksa_java.py ]; then
  python3 tools/periksa_java.py src || { echo "PERIKSA GAGAL: perbaiki dulu"; exit 1; }
fi

NAME="${NAME:-EraaiDailyNews}"
VVERSION="${VERSION_CODE:-1}"
NVERSION="${VERSION_NAME:-1.0}"
rm -rf work out; mkdir -p work/gen work/classes work/obj out

"$BT/aapt2" compile --dir res -o work/res.zip
"$BT/aapt2" link -o work/base.apk \
  -I "$JAR" --manifest AndroidManifest.xml -R work/res.zip \
  --java work/gen --min-sdk-version 21 --target-sdk-version "$TARGET" \
  --version-code "$VVERSION" --version-name "$NVERSION" --auto-add-overlay

find work/gen src -name '*.java' > work/sources.txt
echo "berkas sumber: $(wc -l < work/sources.txt)"
javac -Xlint:-options -source 8 -target 8 -bootclasspath "$JAR" \
  -d work/classes @work/sources.txt

find work/classes -name '*.class' > work/cls.txt
"$BT/d8" --min-api 21 --lib "$JAR" --output work/obj @work/cls.txt
cp work/obj/classes.dex work/classes.dex

cd work
cp base.apk app.unsigned.apk
"$BT/aapt" add app.unsigned.apk classes.dex
"$BT/zipalign" -f 4 app.unsigned.apk app.aligned.apk

if [ -n "${KUNCI_B64:-}" ]; then
  echo "memakai kunci permanen dari secret"
  echo "$KUNCI_B64" | base64 -d > rakit.keystore
  SPASS="$KUNCI_STOREPASS"
  KPASS="$KUNCI_KEYPASS"
else
  echo "memakai kunci sementara (perakitan uji)"
  keytool -genkeypair -keystore rakit.keystore -alias rakit \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass sementara -keypass sementara \
    -dname "CN=Rakit, OU=Rakit, O=Rakit, L=Bandung, C=ID" >/dev/null
  SPASS="sementara"
  KPASS="sementara"
fi

# Tanda tangan v1 + v2 + v3 sekaligus.
#   v1 wajib untuk Android lama, v2 untuk Android 7+, v3 untuk Android 9+.
#   Kalau salah satu kosong, sebagian HP menolak memasang ("aplikasi tidak
#   dapat dipasang"), dan Google Play Protect menandainya sebagai tidak sah.
"$BT/apksigner" sign --ks rakit.keystore --ks-pass "pass:$SPASS" \
  --key-pass "pass:$KPASS" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "../out/${NAME}.apk" app.aligned.apk

echo "--- verifikasi ---"
"$BT/apksigner" verify --print-certs "../out/${NAME}.apk"
echo "--- skema tanda tangan (v1/v2/v3 harus true) ---"
"$BT/apksigner" verify --verbose "../out/${NAME}.apk" | grep -i 'Verified using' || true

# Cek nyata: targetSdk/minSdk yang tertanam + ada tidaknya penanda debug.
echo "--- isi manifes ---"
"$BT/aapt2" dump badging "../out/${NAME}.apk" | grep -i 'sdkVersion\|package:\|application-label' || true
if "$BT/aapt2" dump xmltree "../out/${NAME}.apk" --file AndroidManifest.xml 2>/dev/null | grep -qi debuggable; then
  echo "PERINGATAN: penanda debuggable ada -> Play Protect bisa menolak"
else
  echo "debuggable: tidak ada (baik)"
fi
ls -l ../out/
rm -f rakit.keystore
