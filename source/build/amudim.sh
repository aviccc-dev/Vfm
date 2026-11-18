#!/bin/bash
# בדיקות העמודים של התוספים שבאים עם התוכנה (מבנה 259, 18.11.2026) — רצות בדפדפן מדומה (jsdom), מתוך test.sh.
# עד כאן הורצו ביד, וכישלון בהן לא עצר בנייה (הוראות, סעיף 6: "בנייה אינה נעשית כשבדיקה נכשלת").
# כל שורה: <קובץ הבדיקה שב־lab-pages>:<העמוד שב־assets/plugins>. יוצא בכישלון אם עמוד נכשל, לא רץ, או שהסביבה חסרה.
DIR=$1
T=/home/claude/tools
L=$(dirname "$DIR")/lab-pages
if ! command -v node > /dev/null || [ ! -d "$T/node_modules/jsdom" ]; then
  echo "בדיקות העמודים: חסר node או jsdom — להריץ: bash source/build/sviva.sh"
  exit 1
fi
F=0
for pair in "itzuv-bdika.js:itzuv.html" "orech-bdika.js:orech-interaktivi.html"; do
  js=${pair%%:*}
  page=${pair##*:}
  OUT=$(NODE_PATH=$T/node_modules timeout 300 node "$L/$js" "$DIR/assets/plugins/$page" 2>&1)
  ok=$(echo "$OUT" | grep -c '^✅')
  bad=$(echo "$OUT" | grep -c '^❌\|שגיאות עמוד')
  if [ "$bad" -ne 0 ] || [ "$ok" -eq 0 ]; then
    echo "$OUT" | grep -v '^✅' | head -20
    F=1
    [ "$ok" -eq 0 ] && bad=$((bad + 1))
  fi
  echo "בדיקות העמוד $page: $ok עברו · $bad נכשלו"
done
exit $F
