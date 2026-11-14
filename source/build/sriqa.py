#!/usr/bin/env python3
# הסריקה (הבדיקה הגדולה, חלק 1 — claude/mivne-nisui.md סעיף 9): בדיקה מכנית של כל הקוד בבת אחת.
# לא מתקנת דבר ואינה חלק מהתוכנה: מסמנת חשודים, כדי לדעת איפה לקרוא לעומק. כל ממצא כאן הוא "חשוד", לא "באג".
# שימוש: python3 source/build/sriqa.py <תיקיית source> <קובץ הדוח.md>
# מה נסרק: (1) קטעי קוד שחוזרים · (2) פונקציות וקבועים שאיש לא פונה אליהם · (3) אותו טקסט פעמיים במילון
#          (4) מסד שנפתח ולא נסגר ב־finally · (5) גדלים: הקבצים והפונקציות הארוכים · (6) אזהרות המהדר (אם javac זמין)
import io, os, re, subprocess, sys, tempfile
from collections import defaultdict

SRC = sys.argv[1]
OUT = sys.argv[2]
CODE = os.path.join(SRC, 'src', 'com', 'tikiyot', 'vfm')
K = 8                 # קטע שחוזר: לפחות 8 שורות קוד רצופות זהות
MIN_CHARS = 260       # ...ולפחות כך וכך תווים (שלא ייתפסו רצפים של שורות קצרות)


def read(p):
    return io.open(p, encoding='utf-8').read()


def java_files(root):
    out = []
    for d, _, fs in os.walk(root):
        for f in fs:
            if f.endswith('.java'):
                out.append(os.path.join(d, f))
    return sorted(out)


def rel(p):
    return os.path.relpath(p, CODE)


def strip_comments(text):
    """מסיר הערות ומחרוזות (המחרוזת נשארת כ־""), שומר על מספרי השורות."""
    out, i, n = [], 0, len(text)
    while i < n:
        c = text[i]
        if text.startswith('//', i):
            j = text.find('\n', i)
            i = n if j < 0 else j
        elif text.startswith('/*', i):
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append('\n' * text.count('\n', i, j))
            i = j
        elif c == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == '\\' else 1
            out.append('""')
            i = j + 1
        elif c == "'":
            j = i + 1
            while j < n and text[j] != "'":
                j += 2 if text[j] == '\\' else 1
            out.append("''")
            i = j + 1
        else:
            out.append(c)
            i += 1
    return ''.join(out)


FILES = java_files(CODE)
RAW = {p: read(p) for p in FILES}
BARE = {p: strip_comments(RAW[p]) for p in FILES}
TESTS = java_files(os.path.join(SRC, 'tests'))
TEST_TEXT = '\n'.join(read(p) for p in TESTS)
OTHER = []                                   # עמודי התוספים וקובצי הפקודות — מי שפונה לקוד בשם (הגשר)
for root in (os.path.join(SRC, 'assets'), os.path.join(SRC, '..', 'lab-pages'), os.path.join(SRC, 'AndroidManifest.xml')):
    if os.path.isfile(root):
        OTHER.append(read(root))
    for d, _, fs in os.walk(root):
        for f in fs:
            if f.endswith(('.html', '.template', '.js', '.xml')):
                try:
                    OTHER.append(read(os.path.join(d, f)))
                except Exception:
                    pass
OTHER_TEXT = '\n'.join(OTHER)
report = []


def h(title):
    report.append('\n## ' + title + '\n')


# ---------- 1. קטעי קוד שחוזרים ----------
NOISE = re.compile(r'^(?:[{}()\[\];,]*|@Override|else \{|\} else \{|try \{|\} finally \{|return;|break;|continue;|import .*|package .*)$')


def norm_lines(p):
    out = []
    for no, line in enumerate(BARE[p].split('\n'), 1):
        t = re.sub(r'\s+', ' ', line.strip())
        if not t or NOISE.match(t):
            continue
        out.append((no, t))
    return out


NL = {p: norm_lines(p) for p in FILES}
windows = defaultdict(list)
for p in FILES:
    ls = NL[p]
    for i in range(len(ls) - K + 1):
        key = '\n'.join(t for _, t in ls[i:i + K])
        if len(key) >= MIN_CHARS:
            windows[hash(key)].append((p, i))
pairs = defaultdict(list)                    # (קובץ א, קובץ ב, הפרש) -> אינדקסים
for locs in windows.values():
    if len(locs) < 2 or len(locs) > 6:
        continue
    for a in range(len(locs)):
        for b in range(a + 1, len(locs)):
            (pa, ia), (pb, ib) = locs[a], locs[b]
            if pa == pb and abs(ia - ib) < K:
                continue
            pairs[(pa, pb, ib - ia)].append(ia)
dups = []
for (pa, pb, d), idx in pairs.items():
    idx = sorted(set(idx))
    start = prev = idx[0]
    for i in idx[1:] + [None]:
        if i is not None and i == prev + 1:
            prev = i
            continue
        n = prev - start + K
        la, lb = NL[pa][start][0], NL[pb][start + d][0]
        ea = NL[pa][min(start + n - 1, len(NL[pa]) - 1)][0]
        dups.append((n, rel(pa), la, ea, rel(pb), lb))
        if i is not None:
            start = prev = i
dups.sort(key=lambda x: (-x[0], x[1], x[2]))
seen, shown = set(), []
for n, a, la, ea, b, lb in dups:               # אותו קטע מכמה זוויות — פעם אחת
    k1, k2 = (a, la // 5), (b, lb // 5)
    if k1 in seen and k2 in seen:
        continue
    seen.add(k1)
    seen.add(k2)
    shown.append((n, a, la, ea, b, lb))
h('1. קטעי קוד שחוזרים (%d שורות קוד זהות ומעלה, בלי הערות ורווחים)' % K)
report.append('נמצאו %d קטעים; סך הכול כ־%d שורות כפולות.\n' % (len(shown), sum(x[0] for x in shown)))
report.append('| שורות | מקום א | מקום ב |\n|---|---|---|')
for n, a, la, ea, b, lb in shown[:60]:
    report.append('| %d | `%s` %d–%d | `%s` %d |' % (n, a, la, ea, b, lb))
if len(shown) > 60:
    report.append('| … | ועוד %d קטעים קצרים יותר | |' % (len(shown) - 60))
SUM = ['קטעים שחוזרים: %d (כ־%d שורות); הגדול: %s' % (len(shown), sum(x[0] for x in shown),
       ('%d שורות — %s:%d ↔ %s:%d' % (shown[0][0], shown[0][1], shown[0][2], shown[0][4], shown[0][5])) if shown else '—')]

# ---------- 1ב. קטעים דומים: אותו מבנה, שמות ומספרים אחרים ----------
K2, MIN2 = 6, 90                             # המילון (Words) בחוץ: קבוצות מילים מקבילות דומות זו לזו מטבען
JAVA_WORDS = set('if else for while do switch case default break continue return try catch finally throw throws new null true false this super '
                 'static final private public protected void int long boolean String char double float byte short class interface extends '
                 'implements instanceof synchronized'.split())


def shape(t):
    t = re.sub(r'\b\d[\w.]*\b', 'N', t)
    return re.sub(r'[A-Za-z_$][\w$]*', lambda m: m.group(0) if m.group(0) in JAVA_WORDS else 'I', t)


SH = {p: ([] if p.endswith(os.sep + 'Words.java') else [(no, shape(t)) for no, t in NL[p]]) for p in FILES}
win2 = defaultdict(list)
for p in FILES:
    ls = SH[p]
    for i in range(len(ls) - K2 + 1):
        key = '\n'.join(t for _, t in ls[i:i + K2])
        if len(key) >= MIN2:
            win2[hash(key)].append((p, i))
pairs2 = defaultdict(list)
often = []                                   # דפוס שחוזר ב־6 מקומות ומעלה — לא זוגות, אלא "אותה צורה שוב ושוב"
for locs in win2.values():
    if len(locs) > 5:
        often.append(locs)
        continue
    if len(locs) < 2:
        continue
    for a in range(len(locs)):
        for b in range(a + 1, len(locs)):
            (pa, ia), (pb, ib) = locs[a], locs[b]
            if pa == pb and abs(ia - ib) < K2:
                continue
            pairs2[(pa, pb, ib - ia)].append(ia)
sim = []
for (pa, pb, d), idx in pairs2.items():
    idx = sorted(set(idx))
    start = prev = idx[0]
    for i in idx[1:] + [None]:
        if i is not None and i == prev + 1:
            prev = i
            continue
        n = prev - start + K2
        sim.append((n, rel(pa), SH[pa][start][0], rel(pb), SH[pb][start + d][0]))
        if i is not None:
            start = prev = i
sim.sort(key=lambda x: (-x[0], x[1], x[2]))
seen2, shown2 = set(), []
for n, a, la, b, lb in sim:
    k1, k2 = (a, la // 8), (b, lb // 8)
    if k1 in seen2 and k2 in seen2:
        continue
    seen2.add(k1)
    seen2.add(k2)
    shown2.append((n, a, la, b, lb))
h('1ב. קטעים דומים — אותו מבנה, שמות ומספרים אחרים (%d שורות ומעלה)' % K2)
report.append('נמצאו %d קטעים; סך הכול כ־%d שורות. זה המקום לחפש "אותה פונקציה פעמיים".\n' % (len(shown2), sum(x[0] for x in shown2)))
report.append('| שורות | מקום א | מקום ב |\n|---|---|---|')
for n, a, la, b, lb in shown2[:50]:
    report.append('| %d | `%s` %d | `%s` %d |' % (n, a, la, b, lb))
if len(shown2) > 50:
    report.append('| … | ועוד %d | |' % (len(shown2) - 50))
SUM.append('קטעים דומים במבנה: %d (כ־%d שורות); הגדול: %s' % (len(shown2), sum(x[0] for x in shown2),
           ('%d שורות — %s:%d ↔ %s:%d' % shown2[0]) if shown2 else '—'))
often.sort(key=lambda l: -len(l))
taken, pats = set(), []
for locs in often:                             # חלונות חופפים של אותו דפוס — פעם אחת
    fp, fi = locs[0]
    if any((fp, fi + d) in taken for d in range(-K2, K2 + 1)):
        continue
    taken.add((fp, fi))
    files_n = len(set(pp for pp, _ in locs))
    pats.append((len(locs), files_n, [(rel(pp), SH[pp][ii][0]) for pp, ii in locs[:5]]))
h('1ג. דפוסים שחוזרים הרבה — אותה צורה של %d שורות ב־6 מקומות ומעלה' % K2)
report.append('נמצאו %d דפוסים. כל אחד מועמד ל"פונקציה אחת במקום N העתקים".\n' % len(pats))
report.append('| מקומות | קבצים | לדוגמה |\n|---|---|---|')
for n, fn, ex in pats[:40]:
    report.append('| %d | %d | %s |' % (n, fn, ' · '.join('`%s` %d' % e for e in ex)))
SUM.append('דפוסים שחוזרים ב־6 מקומות ומעלה: %d; הנפוץ: %s' % (len(pats), ('%d מקומות ב־%d קבצים' % pats[0][:2]) if pats else '—'))

# ---------- 2. פונקציות וקבועים שאיש לא פונה אליהם ----------
ALL_BARE = '\n'.join(BARE.values())
# הגדרה של פונקציה: בהזחה של 4 או 8 רווחים בדיוק, ואחריה מיד סוג (לא רווח — אחרת כל קריאה מוזחת נראית כמו הגדרה)
METHOD = re.compile(r'^(?P<ind>    |        )(?P<mods>(?:(?:public|private|protected|static|final|synchronized|abstract)\s+)*)'
                    r'(?:<[^>]+>\s+)?(?P<type>[A-Za-z_][\w.<>\[\],?]*(?: [\w.<>\[\],?]+)*?)\s+(?P<name>\w+)\s*\(', re.M)
KEYWORDS = {'if', 'else', 'for', 'while', 'do', 'switch', 'case', 'catch', 'return', 'throw', 'new', 'synchronized', 'try', 'finally'}
CONST = re.compile(r'\bstatic\s+final\s+[\w.<>\[\], ?]+\s+([A-Z][A-Z0-9_]*(?:\s*=\s*[^;,]+)?(?:\s*,\s*[A-Z][A-Z0-9_]*(?:\s*=\s*[^;,]+)?)*)\s*;')
KEEP = {'main', 'run', 'toString', 'equals', 'hashCode', 'compare', 'compareTo', 'close', 'onCreate', 'query', 'insert', 'update',
        'delete', 'getType', 'openFile'}


def count(name, text):
    return len(re.findall(r'(?<![\w$])' + re.escape(name) + r'(?![\w$])', text))


TOTAL = {}


def total(name):
    if name not in TOTAL:
        TOTAL[name] = count(name, ALL_BARE)
    return TOTAL[name]



def declarations(p):
    """הגדרות של פונקציות בקובץ: (שם, שורה). לא קריאה ("else f(...)"), ולא בנאי."""
    out = []
    for m in METHOD.finditer(BARE[p]):
        name = m.group('name')
        if name in KEYWORDS or name[0].isupper() or set(m.group('type').split()) & KEYWORDS:
            continue
        out.append((name, BARE[p].count('\n', 0, m.start()) + 1))
    return out


DECL = {p: declarations(p) for p in FILES}
DECL_COUNT = defaultdict(int)
for p in FILES:
    for name, _ in DECL[p]:
        DECL_COUNT[name] += 1
CALLS = {}


def calls(name):
    """כמה פעמים השם מופיע בקוד כקריאה — כל ההופעות של "שם(" פחות ההגדרות."""
    if name not in CALLS:
        CALLS[name] = len(re.findall(r'(?<![\w$])' + re.escape(name) + r'\s*\(', ALL_BARE)) - DECL_COUNT[name]
    return CALLS[name]


dead_m, dead_c, tests_only = [], [], []
n_methods = n_consts = 0
for p in FILES:
    raw_lines = RAW[p].split('\n')
    for name, no in DECL[p]:
        if name in KEEP:
            continue
        before = ' '.join(raw_lines[max(0, no - 3):no - 1])
        if '@Override' in before or 'JavascriptInterface' in before:
            continue
        n_methods += 1
        if calls(name) > 0 or count(name, OTHER_TEXT) > 0:
            continue
        (tests_only if count(name, TEST_TEXT) > 0 else dead_m).append((rel(p), no, name))
    for m in CONST.finditer(BARE[p]):
        no = BARE[p].count('\n', 0, m.start()) + 1
        for name in re.findall(r'(?:^|,)\s*([A-Z][A-Z0-9_]*)', m.group(1)):
            n_consts += 1
            if total(name) > 1 or count(name, OTHER_TEXT) > 0:
                continue
            (tests_only if count(name, TEST_TEXT) > 0 else dead_c).append((rel(p), no, name))
h('2. פונקציות וקבועים שאיש לא פונה אליהם')
report.append('נבדקו %d פונקציות ו־%d קבועים, לפי השם: פונקציה — ששום "שם(" בקוד אינו קריאה אליה; קבוע — ששמו מופיע רק בהגדרה. שם שמשותף לכמה פונקציות נספר יחד; פונקציה של הגשר (JavascriptInterface) ו־Override — לא נספרות.\n' % (n_methods, n_consts))
report.append('**פונקציות (%d):** ' % len(dead_m) + ' · '.join('`%s:%d %s`' % x for x in dead_m))
report.append('\n**קבועים (%d):** ' % len(dead_c) + ' · '.join('`%s:%d %s`' % x for x in dead_c))
report.append('\n**רק הבדיקות פונות אליהם (%d):** ' % len(tests_only) + ' · '.join('`%s:%d %s`' % x for x in tests_only))
SUM.append('בלי פונים: %d פונקציות (מ־%d), %d קבועים (מ־%d); רק מהבדיקות: %d' % (len(dead_m), n_methods, len(dead_c), n_consts, len(tests_only)))

# ---------- 2ב. לפי המחלקה: פונקציה סטטית שאיש לא פונה אליה, גם כששמה משותף לפונקציות אחרות ----------
from collections import Counter
QCALL = re.compile(r'(?<![\w$])(\w+)\.(\w+)\s*\(')
BCALL = re.compile(r'(?<![\w$.])(\w+)\s*\(')
Q_FILE = {p: Counter(QCALL.findall(BARE[p])) for p in FILES}
B_FILE = {p: Counter(BCALL.findall(BARE[p])) for p in FILES}
Q_ALL = Counter()
for p in FILES:
    Q_ALL.update(Q_FILE[p])
Q_TESTS = Counter(QCALL.findall(strip_comments(TEST_TEXT)))
dead_s, tests_s = [], []
for p in FILES:
    C = os.path.basename(p)[:-5]
    if C == 'Words':
        continue
    here = Counter(name for name, _ in DECL[p])
    raw_lines = RAW[p].split('\n')
    for m in METHOD.finditer(BARE[p]):
        name = m.group('name')
        if m.group('ind') != '    ' or 'static' not in m.group('mods') or name in KEYWORDS or name in KEEP or name[0].isupper():
            continue
        if set(m.group('type').split()) & KEYWORDS:
            continue
        ext = Q_ALL[(C, name)] - Q_FILE[p][(C, name)]
        inside = B_FILE[p][name] - here[name] + Q_FILE[p][(C, name)]
        if ext > 0 or inside > 0 or count(C + '.' + name, OTHER_TEXT) > 0:
            continue
        no = BARE[p].count('\n', 0, m.start()) + 1
        (tests_s if Q_TESTS[(C, name)] > 0 else dead_s).append((rel(p), no, name))
h('2ב. פונקציות סטטיות שאיש לא פונה אליהן — לפי המחלקה (גם כששם הפונקציה משותף לאחרות)')
report.append('**בלי פונים (%d):** ' % len(dead_s) + ' · '.join('`%s:%d %s`' % x for x in dead_s))
report.append('\n**רק הבדיקות פונות (%d):** ' % len(tests_s) + ' · '.join('`%s:%d %s`' % x for x in tests_s))
SUM.append('פונקציות סטטיות בלי פונים (לפי המחלקה): %d; רק מהבדיקות: %d' % (len(dead_s), len(tests_s)))

# ---------- 2ג. מילים במילון שאיש לא משתמש בהן (לפי הקבוצה שבמילון) ----------
unused_w = []
if [p for p in FILES if p.endswith(os.sep + 'Words.java')]:
    wfile = [p for p in FILES if p.endswith(os.sep + 'Words.java')][0]
    wb = BARE[wfile]
    outside = '\n'.join(BARE[p] for p in FILES if p != wfile) + '\n' + strip_comments(TEST_TEXT)
    W3 = Counter(re.findall(r'(?<![\w$])Words\.(\w+)\.(\w+)', outside))
    W2 = Counter(re.findall(r'(?<![\w$])Words\.(\w+)(?![\w$.])', outside))            # Words.NAME — מילה שלא בקבוצה
    groups = []                                                                          # (שם, התחלה, סוף)
    for m in re.finditer(r'^    (?:public |private )?(?:static )?(?:final )?class (\w+)[^{]*\{', wb, re.M):
        depth, k = 0, m.end() - 1
        while k < len(wb):
            if wb[k] == '{':
                depth += 1
            elif wb[k] == '}':
                depth -= 1
                if depth == 0:
                    break
            k += 1
        groups.append((m.group(1), m.end(), k))
    n_words = 0
    for g, a, b in groups:
        body = wb[a:b]
        rest = wb[:a] + wb[b:]
        names = []
        for m in CONST.finditer(body):
            for name in re.findall(r'(?:^|,)\s*([A-Z][A-Z0-9_]*)', m.group(1)):
                names.append((name, wb.count('\n', 0, a + m.start()) + 1))
        for m in re.finditer(r'^        (?:public |private )?static [\w.<>\[\], ?]+ (\w+)\s*\(', body, re.M):
            names.append((m.group(1), wb.count('\n', 0, a + m.start()) + 1))
        seen_n = set()
        for name, no in names:
            if name in seen_n:
                continue
            seen_n.add(name)
            n_words += 1
            used = W3[(g, name)] + len(re.findall(r'(?<![\w$.])' + re.escape(name) + r'(?![\w$])', body)) - 1 \
                + len(re.findall(r'(?<![\w$])' + re.escape(g) + r'\.' + re.escape(name) + r'(?![\w$])', rest))
            if used <= 0:
                unused_w.append(('%s.%s' % (g, name), no))
    h('2ג. מילים במילון שאיש לא משתמש בהן (נבדקו %d, לפי הקבוצה: Words.<קבוצה>.<מילה>)' % n_words)
    report.append(' · '.join('`%s` (%d)' % x for x in unused_w) if unused_w else 'לא נמצא.')
    SUM.append('מילים במילון בלי שימוש: %d (מ־%d)' % (len(unused_w), n_words))

# ---------- 3. אותו טקסט פעמיים במילון ----------
wp = [p for p in FILES if p.endswith(os.sep + 'Words.java')]
same = []
if wp:
    lit = defaultdict(list)
    for m in re.finditer(r'\b([A-Z][A-Z0-9_]*)\s*=\s*"((?:[^"\\]|\\.)*)"\s*[;,]', RAW[wp[0]]):
        if len(m.group(2)) >= 6:
            lit[m.group(2)].append((RAW[wp[0]].count('\n', 0, m.start()) + 1, m.group(1)))
    same = sorted(((v, k) for k, v in lit.items() if len(v) > 1), key=lambda x: -len(x[0]))
h('3. אותו טקסט פעמיים במילון (קבועים בני 6 תווים ומעלה)')
report.append('נמצאו %d טקסטים.\n' % len(same))
for v, k in same[:50]:
    report.append('- "%s" — %s' % (k[:70], ' · '.join('%s (שורה %d)' % (n, no) for no, n in v)))
SUM.append('אותו טקסט פעמיים במילון: %d' % len(same))

# ---------- 4. מסד שנפתח ולא נסגר ב־finally ----------
leaks = []
for p in FILES:
    lines = BARE[p].split('\n')
    for no, line in enumerate(lines, 1):
        m = re.search(r'(\w+)\s*=\s*(?:Db\.open(?:Read)?|SqlStore\.open(?:ReadOnly)?)\(', line)
        if not m:
            continue
        var = m.group(1)
        tail = '\n'.join(lines[no:no + 120])
        fin = re.search(r'finally\s*\{[^}]*\b' + re.escape(var) + r'\s*(?:!=\s*null\s*\)\s*' + re.escape(var) + r')?\.close\(\)', tail)
        if not fin:
            leaks.append((rel(p), no, var))
h('4. מסד שנפתח, ואין אחריו סגירה ב־finally (ב־120 השורות הבאות)')
report.append(' · '.join('`%s:%d %s`' % x for x in leaks) if leaks else 'לא נמצא.')
SUM.append('פתיחת מסד בלי סגירה ב־finally: %d' % len(leaks))

# ---------- 5. גדלים ----------
sizes = sorted(((RAW[p].count('\n') + 1, rel(p)) for p in FILES), reverse=True)
long_m = []
for p in FILES:
    text = BARE[p]
    for m in METHOD.finditer(text):
        j = text.find('{', m.end())
        semi = text.find(';', m.end())
        if j < 0 or (0 <= semi < j):
            continue
        depth, k = 0, j
        while k < len(text):
            if text[k] == '{':
                depth += 1
            elif text[k] == '}':
                depth -= 1
                if depth == 0:
                    break
            k += 1
        n = text.count('\n', j, k) + 1
        if n >= 90 and m.group('name') not in KEYWORDS and not (set(m.group('type').split()) & KEYWORDS):
            long_m.append((n, rel(p), text.count('\n', 0, m.start()) + 1, m.group('name')))
long_m.sort(reverse=True)
h('5. גדלים')
report.append('**קבצים:** %d · **שורות:** %d\n' % (len(FILES), sum(s for s, _ in sizes)))
report.append('**הקבצים הגדולים:** ' + ' · '.join('`%s` %d' % (n, s) for s, n in sizes[:15]))
report.append('\n**פונקציות של 90 שורות ומעלה (%d):** ' % len(long_m) + ' · '.join('`%s:%d %s` %d' % (f, no, name, n) for n, f, no, name in long_m[:40]))
by_floor = defaultdict(lambda: [0, 0])
for s, n in sizes:
    f = n.split(os.sep)[0]
    by_floor[f][0] += 1
    by_floor[f][1] += s
report.append('\n**לפי קומה:** ' + ' · '.join('%s — %d קבצים, %d שורות' % (f, by_floor[f][0], by_floor[f][1]) for f in ('basis', 'chomer', 'pkudot', 'lev', 'masachim')))
SUM.append('קבצים: %d · שורות: %d · פונקציות של 90+ שורות: %d' % (len(FILES), sum(s for s, _ in sizes), len(long_m)))

# ---------- 6. אזהרות המהדר ----------
jar = '/home/claude/tools/android.jar'
h('6. אזהרות המהדר (javac -Xlint:all)')
if os.path.isfile(jar):
    out = tempfile.mkdtemp()
    r = subprocess.run(['javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-Xlint:all', '-Xlint:-options', '-Xmaxwarns', '5000',
                        '-bootclasspath', jar, '-classpath', jar, '-d', out] + FILES, capture_output=True, text=True)
    kinds, where = defaultdict(int), defaultdict(lambda: defaultdict(int))
    for line in r.stderr.split('\n'):
        m = re.match(r'(.*?\.java):(\d+): warning: \[(\w+)\]', line)
        if m:
            kinds[m.group(3)] += 1
            where[m.group(3)][os.path.basename(m.group(1))[:-5]] += 1
    if not kinds and r.returncode != 0:
        report.append('המהדר נכשל: ' + r.stderr[:300])
    for k, n in sorted(kinds.items(), key=lambda x: -x[1]):
        top = sorted(where[k].items(), key=lambda x: -x[1])[:8]
        report.append('- **%s** — %d: %s' % (k, n, ' · '.join('%s %d' % t for t in top)))
    SUM.append('אזהרות המהדר: ' + (', '.join('%s %d' % (k, n) for k, n in sorted(kinds.items(), key=lambda x: -x[1])) or 'אין'))
else:
    report.append('לא רץ — android.jar חסר (bash source/build/sviva.sh).')

head = ['# הסריקה — בדיקה מכנית של הקוד (נוצר ב־`source/build/sriqa.py`; לא לערוך ביד)\n',
        'כל שורה כאן היא **חשוד**, לא באג: המכונה מסמנת איפה כדאי לקרוא. ההכרעה — בקריאה (חלק 2 של הבדיקה הגדולה).\n',
        '**בקצרה:**'] + ['- ' + s for s in SUM]
io.open(OUT, 'w', encoding='utf-8').write('\n'.join(head + report) + '\n')
print('\n'.join(SUM))
