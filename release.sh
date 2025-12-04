#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="Pomodone"
MAIN_CLASS="com.pomodone.app.MainApp"
DIST_DIR="$ROOT_DIR/dist"
INPUT_DIR="$ROOT_DIR/target/jpackage-input"
JAVAFX_DIR="$INPUT_DIR/javafx-mods"

detect_os() {
    case "$(uname -s)" in
        Darwin) echo "macos" ;;
        Linux) echo "linux" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
        *) echo "unknown" ;;
    esac
}

OS_TYPE="$(detect_os)"

# --- KONFIGURASI TIPE PAKET ---
# Jika user memberikan input spesifik via env var, pakai itu.
# Jika tidak, tentukan default berdasarkan OS.
# Untuk Linux, set default array berisi "deb" DAN "rpm".
PACKAGE_TYPES=()

if [ -n "${JPACKAGE_TYPE:-}" ]; then
    PACKAGE_TYPES=("$JPACKAGE_TYPE")
else
    case "$OS_TYPE" in
        macos) PACKAGE_TYPES=("dmg") ;;
        windows) PACKAGE_TYPES=("msi") ;;
        linux)
            if [ -f /etc/os-release ]; then
                . /etc/os-release
                if [[ "$ID" == "fedora" || "$ID_LIKE" == *"fedora"* ]]; then
                    PACKAGE_TYPES=("rpm")
                else
                    PACKAGE_TYPES=("deb" "rpm")
                fi
            else
                PACKAGE_TYPES=("deb" "rpm")
            fi
            ;;
        *) PACKAGE_TYPES=("app-image") ;;
    esac
fi

# Check for rpm-build if we are building rpm
if [[ " ${PACKAGE_TYPES[*]} " =~ " rpm " ]]; then
    if ! command -v rpmbuild >/dev/null 2>&1; then
        echo "rpmbuild tidak ditemukan. Install dengan: sudo dnf install rpm-build" >&2
        exit 1
    fi
fi

if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven (mvn) tidak ditemukan di PATH." >&2
    exit 1
fi

if ! command -v jpackage >/dev/null 2>&1; then
    echo "jpackage (JDK 17+) tidak ditemukan di PATH." >&2
    exit 1
fi

prepare_icon() {
    local icon_dir="$ROOT_DIR/src/main/resources/images"
    local icon_path=""

    case "$OS_TYPE" in
        macos)
            icon_path="$icon_dir/icon.icns"
            if [ -f "$icon_path" ]; then
                echo "$icon_path"
                return
            fi

            if command -v sips >/dev/null 2>&1 && command -v iconutil >/dev/null 2>&1; then
                local base="$icon_dir/icon.png"
                local tmp_base=""
                if [ ! -f "$base" ] && [ -f "$icon_dir/icon.jpeg" ]; then
                    tmp_base="$ROOT_DIR/target/icon_base.png"
                    sips -s format png "$icon_dir/icon.jpeg" --out "$tmp_base" >/dev/null
                    base="$tmp_base"
                fi
                if [ -f "$base" ]; then
                    local iconset="$ROOT_DIR/target/icon.iconset"
                    rm -rf "$iconset"
                    mkdir -p "$iconset"
                    local sizes=(16 32 64 128 256 512 1024)
                    for size in "${sizes[@]}"; do
                        sips -z "$size" "$size" "$base" --out "$iconset/icon_${size}x${size}.png" >/dev/null
                    done
                    cp "$iconset/icon_32x32.png" "$iconset/icon_16x16@2x.png" 2>/dev/null || true
                    cp "$iconset/icon_64x64.png" "$iconset/icon_32x32@2x.png" 2>/dev/null || true
                    cp "$iconset/icon_128x128.png" "$iconset/icon_64x64@2x.png" 2>/dev/null || true
                    cp "$iconset/icon_256x256.png" "$iconset/icon_128x128@2x.png" 2>/dev/null || true
                    cp "$iconset/icon_512x512.png" "$iconset/icon_256x256@2x.png" 2>/dev/null || true
                    cp "$iconset/icon_1024x1024.png" "$iconset/icon_512x512@2x.png" 2>/dev/null || true

                    iconutil -c icns "$iconset" -o "$icon_path" >/dev/null || true
                    rm -rf "$iconset" "$tmp_base"
                    if [ -f "$icon_path" ]; then
                        echo "$icon_path"
                        return
                    fi
                fi
            fi
            echo "" ;;
        linux)
            if [ -f "$icon_dir/icon.png" ]; then
                echo "$icon_dir/icon.png"
                return
            fi
            if [ -f "$icon_dir/icon.jpeg" ] && command -v sips >/dev/null 2>&1; then
                local png_path="$icon_dir/icon.png"
                sips -s format png "$icon_dir/icon.jpeg" --out "$png_path" >/dev/null || true
                [ -f "$png_path" ] && echo "$png_path" && return
            fi
            echo "" ;;
        windows)
            if [ -f "$icon_dir/icon.ico" ]; then
                echo "$icon_dir/icon.ico"
                return
            fi
            echo "" ;;
        *)
            echo "" ;;
    esac
}

ICON_PATH="$(prepare_icon)"

sanitize_version() {
    local ver="$1"
    ver="${ver%-SNAPSHOT}"
    ver="${ver%-snapshot}"
    ver="${ver#v}"
    if [[ "$ver" =~ ^0[0-9]*(\..*)?$ ]]; then
        ver="1${ver#0}"
        [[ "$ver" == "1" || "$ver" == "1." ]] && ver="1.0.0"
    fi
    if ! [[ "$ver" =~ ^[0-9]+(\.[0-9]+){0,2}$ ]]; then
        ver="1.0.0"
    fi
    echo "$ver"
}

ARTIFACT_ID="$(mvn -q -DforceStdout -Dexpression=project.artifactId help:evaluate)"
VERSION="$(mvn -q -DforceStdout -Dexpression=project.version help:evaluate)"
MAIN_JAR="${ARTIFACT_ID}-${VERSION}.jar"
APP_VERSION="$(sanitize_version "$VERSION")"

rm -rf "$INPUT_DIR" "$DIST_DIR"
mkdir -p "$INPUT_DIR" "$DIST_DIR"

echo "Building project dan menyalin dependencies..."
mvn -B clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$INPUT_DIR"
cp "$ROOT_DIR/target/$MAIN_JAR" "$INPUT_DIR"

rm -rf "$JAVAFX_DIR"
mkdir -p "$JAVAFX_DIR"
find "$INPUT_DIR" -maxdepth 1 -type f -name "javafx-*.jar" -exec cp {} "$JAVAFX_DIR"/ \;

ADD_MODULES_VALUE="${JPACKAGE_MODULES:-javafx.controls,javafx.fxml,javafx.media}"

JAVA_OPTS=(--module-path "\$APPDIR/javafx-mods" --add-modules "$ADD_MODULES_VALUE" --add-opens java.base/java.lang=ALL-UNNAMED)

# --- LOOPING UNTUK BUILD SEMUA TIPE ---
for TYPE in "${PACKAGE_TYPES[@]}"; do
    echo "=========================================="
    echo "Membungkus native app via jpackage (Tipe: $TYPE)..."
    echo "=========================================="

    jpackage \
      --name "$APP_NAME" \
      --input "$INPUT_DIR" \
      --main-jar "$MAIN_JAR" \
      --main-class "$MAIN_CLASS" \
      --dest "$DIST_DIR" \
      --type "$TYPE" \
      --app-version "$APP_VERSION" \
      --java-options "${JAVA_OPTS[*]}" \
      --linux-shortcut \
      ${ICON_PATH:+--icon "$ICON_PATH"} || {
          echo "GAGAL membuat paket tipe: $TYPE. Pastikan tools (rpm-build/dpkg-deb) terinstall."
          continue
      }
done

echo "=========================================="
echo "Selesai. Lihat output di: $DIST_DIR"

if [ -f /etc/os-release ]; then
    . /etc/os-release
    if [[ "$ID" == "fedora" || "$ID_LIKE" == *"fedora"* ]]; then
        echo "=========================================="
        echo "Deteksi Fedora: Memulai instalasi otomatis..."
        echo "=========================================="
        
        RPM_FILE=$(find "$DIST_DIR" -name "*.rpm" | head -n 1)
        if [ -n "$RPM_FILE" ]; then
            echo "Ditemukan: $RPM_FILE"
            echo "Meminta izin sudo untuk menginstall..."
            sudo dnf install -y "$RPM_FILE"
            
            if [ $? -eq 0 ]; then
                echo "Instalasi BERHASIL!"
                echo "Aplikasi telah ditambahkan ke menu desktop."
                echo "Menjalankan aplikasi sekarang..."
                /opt/pomodone/bin/Pomodone
            else
                echo "Instalasi gagal."
            fi
        else
            echo "File RPM tidak ditemukan."
        fi
    fi
fi