#!/usr/bin/env python3
# בונה את תוסף האבחון מהתבנית: קובצי הבדיקה (source/assets/tests) נכנסים לעמוד, והעמוד נכתב פעמיים —
# "תוסף אבחון <גרסה>.html" כאן, והעותק שבא עם התוכנה (source/assets/plugins/avchon.html).
# שימוש: python3 avchon-build.py <גרסת התוסף>
import json, os, sys

here = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(here, '..', 'source')
ver = sys.argv[1] if len(sys.argv) > 1 else '1'
tests = {}
tdir = os.path.join(src, 'assets', 'tests')
for n in sorted(os.listdir(tdir)):
    if n.endswith('.vbat'):
        tests[n[:-5]] = open(os.path.join(tdir, n), encoding='utf-8').read().replace('\r', '')
data = json.dumps(tests, ensure_ascii=False).replace('</', '<\\/')
t = open(os.path.join(here, 'avchon.template'), encoding='utf-8').read()
assert t.count('/*TESTS*/') == 1 and t.count('/*VERSION*/') == 1
out = t.replace('/*TESTS*/', data).replace('/*VERSION*/', ver)
for p in (os.path.join(here, 'תוסף אבחון ' + ver + '.html'), os.path.join(src, 'assets', 'plugins', 'avchon.html')):
    open(p, 'w', encoding='utf-8').write(out)
    print(p, len(out.encode('utf-8')))
