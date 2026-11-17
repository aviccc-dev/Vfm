# בונה קובץ מפה בצורה של "קבוצות ותגיות" (כמו הקובץ של המשתמש).
import sys, json
from xml.sax.saxutils import escape, quoteattr

def node(n, depth, out):
    pad = '  ' * depth
    text, info, kids = n.get('t'), n.get('info', []), n.get('k', [])
    attrs = ''
    if n.get('div'):
        attrs = ' kvDivision="yes" text=%s kvAspect="b1"' % quoteattr(text)
    else:
        attrs = ' text=%s' % quoteattr(text)
    if not info and not kids:
        out.append('%s<outline%s/>' % (pad, attrs)); return
    out.append('%s<outline%s>' % (pad, attrs))
    for name, body in info:
        out.append('%s  <kvInfo kind="user" name=%s>%s</kvInfo>' % (pad, quoteattr(name), escape(body)))
    for k in kids: node(k, depth + 1, out)
    out.append('%s</outline>' % pad)

def build(spec):
    out = ['קבוצות ותגיות · קובץ מפה', '<?xml version="1.0" encoding="UTF-8"?>', '<opml version="2.0">', '  <head>',
           '    <title>%s</title>' % escape(spec['title']), '    <kv version="2" absolute="yes"/>', '    <kvAspects>',
           '      <kvAspect id="b1" name=%s/>' % quoteattr(spec['aspect']), '    </kvAspects>', '    <kvSheets>',
           '      <kvSheet id="s1" kind="start" name="התחלת נושא"/>', '      <kvSheet id="s2" kind="free" name="סיעור מוחות"/>',
           '      <kvSheet id="s3" kind="map" name="מפה" absolute="yes" body="yes"/>', '    </kvSheets>', '  </head>', '  <body>']
    root = {'t': spec['root'], 'info': spec.get('info', []), 'k': [{'t': spec['aspect'], 'div': True, 'k': spec['k']}]}
    node(root, 2, out)
    out += ['  </body>', '</opml>', '']
    return '\n'.join(out)

spec = json.load(open(sys.argv[1], encoding='utf-8'))
open(sys.argv[2], 'w', encoding='utf-8', newline='\n').write(build(spec))
