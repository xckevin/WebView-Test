#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLAY_DIR="$ROOT_DIR/app/src/main/play/listings"
ICON_SRC="$ROOT_DIR/docs/assets/webviewtest-market-icon-512.png"
FONT="/Library/Fonts/Arial Unicode.ttf"

if ! command -v magick >/dev/null 2>&1; then
  echo "ImageMagick 'magick' command is required." >&2
  exit 1
fi

if [[ ! -f "$FONT" ]]; then
  FONT="/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
fi

locales=(
  en-US
  zh-CN
  ja-JP
  es-ES
  fr-FR
  de-DE
  pt-PT
  ru-RU
)

caption_for_locale() {
  case "$1" in
    en-US) printf '%s' 'Load, inspect, and debug Android WebView pages.' ;;
    zh-CN) printf '%s' '加载、检查和调试 Android WebView 页面。' ;;
    ja-JP) printf '%s' 'Android WebView ページを読み込み、検査、デバッグ。' ;;
    es-ES) printf '%s' 'Carga, inspecciona y depura WebView en Android.' ;;
    fr-FR) printf '%s' 'Chargez, inspectez et déboguez WebView sur Android.' ;;
    de-DE) printf '%s' 'WebView-Seiten auf Android laden, prüfen und debuggen.' ;;
    pt-PT) printf '%s' 'Carregue, inspecione e depure WebView no Android.' ;;
    ru-RU) printf '%s' 'Загрузка, проверка и отладка WebView на Android.' ;;
    *) printf '%s' 'Load, inspect, and debug Android WebView pages.' ;;
  esac
}

for locale in "${locales[@]}"; do
  graphics_dir="$PLAY_DIR/$locale/graphics"
  icon_dir="$graphics_dir/icon"
  feature_dir="$graphics_dir/feature-graphic"
  mkdir -p "$icon_dir" "$feature_dir" "$graphics_dir/phone-screenshots"

  cp "$ICON_SRC" "$icon_dir/1.png"

  caption="$(caption_for_locale "$locale")"
  output="$feature_dir/1.png"

  magick -size 1024x500 xc:'#111827' \
    -fill '#163B45' -draw 'rectangle 0,0 1024,500' \
    -fill '#F7FAFC' -draw 'roundrectangle 440,72 935,428 28,28' \
    -fill '#E8EEF4' -draw 'roundrectangle 466,104 909,158 18,18' \
    -fill '#0F172A' -draw 'roundrectangle 486,119 736,143 12,12' \
    -fill '#4F6B7A' -draw 'circle 874,131 886,131' \
    -fill '#DCFCE7' -draw 'roundrectangle 486,188 676,218 12,12' \
    -fill '#E0F2FE' -draw 'roundrectangle 486,240 830,270 12,12' \
    -fill '#FEF3C7' -draw 'roundrectangle 486,292 776,322 12,12' \
    -fill '#E5E7EB' -draw 'roundrectangle 486,348 710,370 10,10' \
    -fill '#C7D2FE' -draw 'roundrectangle 730,348 860,370 10,10' \
    \( "$ICON_SRC" -resize 128x128 \) -geometry +80+92 -composite \
    -font "$FONT" -fill '#F8FAFC' -pointsize 64 -gravity northwest -annotate +80+242 'WebViewTest' \
    -font "$FONT" -fill '#D1FAE5' -pointsize 30 -gravity northwest -annotate +84+328 "$caption" \
    -font "$FONT" -fill '#BAE6FD' -pointsize 24 -gravity northwest -annotate +84+382 'https://developer.android.com/' \
    -alpha off "PNG24:$output"
done

echo "Generated Play graphics for ${#locales[@]} locales."
