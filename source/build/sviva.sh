#!/bin/bash
# סביבת העבודה בפקודה אחת (מבנה 32, 13.11.2026). הסביבה מתאפסת בין שיחות; הסקריפט מכין רק את מה שחסר,
# ובמה שכבר קיים אינו נוגע — אפשר להריץ אותו שוב ושוב.
# שימוש:  bash source/build/sviva.sh          — מה שצריך לבדיקות המחשב (android.jar)
#         bash source/build/sviva.sh build    — וגם כלי הבנייה של קובץ ההתקנה (aapt, zipalign, apksigner, dalvik-exchange)
# אחר כך: bash source/build/test.sh <תיקיית source המלאה>   ·   bash source/build/build.sh <source> <יעד.apk> m|n
set -e
T=${VFM_TOOLS:-/home/claude/tools}   # test.sh ו־build.sh מחפשים ב־/home/claude/tools; VFM_TOOLS — רק לבדיקת הסקריפט עצמו
mkdir -p "$T"

if [ ! -s "$T/android.jar" ]; then
  D=$(mktemp -d)
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/Sable/android-platforms "$D/p"
  git -C "$D/p" sparse-checkout set android-30
  cp "$D/p/android-30/android.jar" "$T/android.jar"
  rm -rf "$D"
  echo "android.jar: הורד"
else
  echo "android.jar: יש"
fi
[ -s "$T/android.jar" ] || { echo "הסביבה לא מוכנה: android.jar חסר"; exit 1; }

if [ "$1" = "build" ]; then
  NEED=""
  for t in aapt zipalign apksigner dalvik-exchange; do command -v $t > /dev/null || NEED="$NEED $t"; done
  if [ -n "$NEED" ]; then
    apt-get update -qq                     # בלי העדכון — "Unable to locate package" (קרה ב־13.11)
    apt-get install -y -qq --no-install-recommends $NEED > /dev/null
  fi
  for t in aapt zipalign apksigner dalvik-exchange; do
    command -v $t > /dev/null || { echo "הסביבה לא מוכנה: $t חסר"; exit 1; }
  done
  echo "כלי הבנייה: יש"
fi

command -v javac > /dev/null || { echo "הסביבה לא מוכנה: javac חסר"; exit 1; }
echo "java: $(javac -version 2>&1 | tail -1)"
echo "הסביבה מוכנה."
