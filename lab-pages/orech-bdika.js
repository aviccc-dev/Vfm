const {JSDOM}=require('jsdom');const fs=require('fs');
const html=fs.readFileSync(process.argv[2],'utf8');
const errs=[];
const dom=new JSDOM(html,{runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:new (require('jsdom').VirtualConsole)().on('jsdomError',e=>errs.push(String(e.detail||e.message||e).slice(0,300))).on('error',e=>errs.push(String(e).slice(0,300)))});
const w=dom.window,d=w.document;
const $=id=>d.getElementById(id);const click=el=>el.dispatchEvent(new w.MouseEvent('click',{bubbles:true}));
const out=[];const say=(n,ok)=>out.push((ok?'✅ ':'❌ ')+n);
const menuItems=()=>[...$('menu').querySelectorAll('button')];
const openM=m=>click(d.querySelector('#mbar button[data-m="'+m+'"]'));
const pick=(m,t)=>{openM(m);const b=menuItems().find(x=>x.textContent.trim().replace(/^[•✓]/,'').trim()===t);if(!b)throw new Error('אין פריט '+t);if(b.disabled)return false;click(b);return true;};
const rows=()=>[...d.querySelectorAll('#ed .row')].length;
const tb=()=>[...$('tbar').querySelectorAll('button')].map(b=>b.textContent+(b.disabled?'(-)':''));
try{
  say('חמישה תפריטים, בסדר של חלונות',[...d.querySelectorAll('#mbar button[data-m]')].map(b=>b.textContent).join(' ')==='קובץ עריכה תצוגה הרצה עזרה');
  say('סרגל ברירת המחדל: '+tb().join(' | '),tb().length===5);
  openM('file');say('תפריט קובץ: '+menuItems().map(b=>b.textContent.trim()).join(' · '),menuItems().length===7&&!$('menu').hidden);
  openM('file');say('נגיעה שנייה סוגרת',$('menu').hidden);
  const n0=rows();say('קובץ נטען בדוגמה ('+n0+' שורות)',n0>0);
  click(d.querySelectorAll('#ed .row')[0]);
  say('שכפול',pick('edit','שכפול')&&rows()===n0+1);
  say('ביטול',pick('edit','ביטול')&&rows()===n0);
  say('חזרה',pick('edit','חזרה')&&rows()===n0+1);
  pick('edit','ביטול');
  click(d.querySelectorAll('#ed .row')[0]);
  const first=()=>d.querySelectorAll('#ed .row')[0].textContent;
  const f0=first();
  say('הזזה למטה',pick('edit','הזזה למטה')&&rows()===n0);
  click(d.querySelectorAll('#ed .row')[1]);
  say('הזזה למעלה מחזירה',pick('edit','הזזה למעלה'));
  click(d.querySelectorAll('#ed .row')[0]);
  say('גזירה',pick('edit','גזירה')&&rows()===n0-1);
  say('הדבקה',pick('edit','הדבקה')&&rows()===n0);
  say('שורה חדשה מהסרגל',(click([...$('tbar').querySelectorAll('button')].find(b=>b.textContent==='+ שורה')),rows()===n0+1));
  pick('edit','ביטול');
  say('תצוגה ← רגילה',pick('view','רגילה')&&!$('plain').hidden);
  openM('view');say('סימון • ליד "רגילה"',menuItems().find(b=>b.textContent.includes('רגילה')).textContent.trim().startsWith('•'));openM('view');
  say('תצוגה ← אינטראקטיבית',pick('view','אינטראקטיבית')&&$('plain').hidden);
  say('הסתרת הסרגל',pick('view','סרגל קיצורים')&&$('tbar').hidden);
  say('החזרת הסרגל',pick('view','סרגל קיצורים')&&!$('tbar').hidden);
  pick('view','התאמת הקיצורים…');
  const add=[...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.includes('שכפול'));click(add);
  say('התאמת הקיצורים — נוסף "שכפול": '+tb().join(' | '),tb().some(x=>x.startsWith('שכפול')));
  click([...d.querySelectorAll('#sheetIn button')].find(b=>b.textContent==='חזרה לברירת המחדל'));
  say('חזרה לברירת המחדל',tb().length===5);
  click($('shade'));
  pick('edit','חיפוש והחלפה…');
  say('חיפוש והחלפה נפתח',[...d.querySelectorAll('#sheetIn input')].length===2);
  click($('shade'));
  say('הרצה בכאילו',pick('run','הרצה בכאילו'));
  say('עזרה ← על העורך',pick('help','על העורך')&&!!d.querySelector('.toast'));
  // גרסה 18 (ממצא 58): בגיליון של הפקודה — המחיקה ראשונה; בהחלפה — טקסט חופשי אינו נעשה שם, ומה שירד — נאמר
  click($('shade'));
  click(d.querySelectorAll('#ed .row')[0]);click(d.querySelector('#ed .chip.cmd'));
  say('גיליון הפקודה — "מחיקת השורה" ראשונה',([...d.querySelectorAll('#sheetIn .it')][0]||{textContent:''}).textContent.includes('מחיקת'));
  click($('shade'));
  pick('view','רגילה');
  const ta58=d.querySelector('textarea');ta58.value='פתח כלי כפולים /אופן:"בתוך" /השוואה:"שם וגודל"\n';ta58.dispatchEvent(new w.Event('input',{bubbles:true}));
  pick('view','אינטראקטיבית');
  click(d.querySelectorAll('#ed .row')[0]);click(d.querySelector('#ed .chip.cmd'));
  click([...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.startsWith('רשימות')));
  click([...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.trim().startsWith('צור רשימה חכמה')));
  const row58=d.querySelectorAll('#ed .row')[0].textContent,toast58=(d.querySelector('.toast')||{textContent:''}).textContent;
  say('החלפה: ההגדרות של "פתח כלי" לא נעשו שם של רשימה חכמה, ונאמר שירדו',row58.indexOf('אופן')<0&&toast58.includes('/אופן'));
  // גרסה 19: "כפולים…" ליד התנאי של רשימה חכמה — בחירה, ותנאי אחד של כפולים (בחירה שנייה מחליפה)
  pick('view','רגילה');
  const ta59=d.querySelector('textarea');ta59.value='צור רשימה חכמה "כפ" לפי vbat\n';ta59.dispatchEvent(new w.Event('input',{bubbles:true}));
  pick('view','אינטראקטיבית');click(d.querySelectorAll('#ed .row')[0]);
  const dupChip=()=>[...d.querySelectorAll('#ed .chip')].find(b=>b.textContent==='כפולים…');
  say('ליד התנאי של רשימה חכמה — "כפולים…"',!!dupChip());
  click(dupChip());click([...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.startsWith('תוכן')));
  click(d.querySelectorAll('#ed .row')[0]);click(dupChip());
  click([...d.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.startsWith('שם וגודל')));
  pick('view','רגילה');const t59=d.querySelector('textarea').value.trim();pick('view','אינטראקטיבית');
  say('כפולים בתנאי — אחד, האחרון שנבחר: '+t59,t59==='צור רשימה חכמה "כפ" לפי vbat כפולים:שם-וגודל');
  // גרסה 20: מחיקת הקובץ — אישור; "ביטול" לא נוגע; "למחוק" — דרך התוכנה, והעורך בלי קובץ פתוח
  const path20='/k/גיבוי שבועי.vbat';
  say('קובץ ← "מחיקת הקובץ…" — פעיל כשיש קובץ',pick('file','מחיקת הקובץ…')&&d.querySelector('#sheetIn').textContent.includes('יעבור לסל'));
  click([...d.querySelectorAll('#sheetIn button')].find(b=>b.textContent==='ביטול'));
  say('ביטול — הקובץ נשאר פתוח',!w.__deleted&&rows()>0);
  pick('file','מחיקת הקובץ…');
  click([...d.querySelectorAll('#sheetIn button')].find(b=>b.textContent==='למחוק'));
  say('למחוק — נמחק דרך התוכנה ('+w.__deleted+'), ונאמר',w.__deleted===path20&&(d.querySelector('.toast')||{textContent:''}).textContent.includes('עבר לסל'));
  say('אחרי המחיקה — אין קובץ פתוח, ו"מחיקת הקובץ" כבויה',!!d.querySelector('#ed .empty')&&pick('file','מחיקת הקובץ…')===false);
}catch(e){out.push('❌ חריגה: '+e.message);}

// ===== גרסה 21: "עצב" ו"ודא עיצוב" — הפרטים מהתוכנה. עמוד חדש לכל מצב: תוכנה שמכירה, תוכנה שאינה מכירה, ותוכנה כמו מ0.47 =====
function page(flags){
  const er=[];
  const dm=new JSDOM(html,{runScripts:'dangerously',pretendToBeVisual:true,beforeParse(win){Object.assign(win,flags||{});},
    virtualConsole:new (require('jsdom').VirtualConsole)().on('jsdomError',e=>er.push(String(e.detail||e.message||e).slice(0,300))).on('error',e=>er.push(String(e).slice(0,300)))});
  const W=dm.window,D=W.document,g=id=>D.getElementById(id);
  const hit=el=>{if(!el)throw new Error('אין מה ללחוץ');el.dispatchEvent(new W.MouseEvent('click',{bubbles:true}));};
  const mi=()=>[...g('menu').querySelectorAll('button')];
  const mpick=(m,t)=>{hit(D.querySelector('#mbar button[data-m="'+m+'"]'));hit(mi().find(x=>x.textContent.trim().replace(/^[•✓]/,'').trim()===t));};
  return {W:W,D:D,er:er,hit:hit,Mo:W.VfmEditorModel,
    set:t=>{mpick('view','רגילה');const ta=D.querySelector('textarea');ta.value=t;ta.dispatchEvent(new W.Event('input',{bubbles:true}));mpick('view','אינטראקטיבית');},
    get:()=>{mpick('view','רגילה');const t=D.querySelector('textarea').value;mpick('view','אינטראקטיבית');return t.trim();},
    row:i=>D.querySelectorAll('#ed .row')[i],
    chip:(i,t)=>[...D.querySelectorAll('#ed .row')[i].querySelectorAll('.chip')].find(c=>c.textContent===t),
    its:()=>[...D.querySelectorAll('#sheetIn .it')],it:t=>[...D.querySelectorAll('#sheetIn .it')].find(b=>b.textContent.startsWith(t)),
    keys:()=>[...D.querySelectorAll('#sheetIn .key')],key:t=>[...D.querySelectorAll('#sheetIn .key')].find(b=>b.textContent===t),
    btn:t=>[...D.querySelectorAll('#sheetIn button')].find(b=>b.textContent===t),
    type:(f,v)=>{f.value=v;f.dispatchEvent(new W.Event('input',{bubbles:true}));},
    toast:()=>(D.querySelector('.toast')||{textContent:''}).textContent,sheetOpen:()=>!g('sheet').hidden,shade:()=>hit(g('shade')),
    tb:t=>hit([...g('tbar').querySelectorAll('button')].find(b=>b.textContent===t))};
}
const wait=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
  const a=page(),Mo=a.Mo;
  say('עצב: הפרטים באים מהתוכנה ('+Mo.CMDS['עצב'].det.length+'), ושתי הפקודות בתפריט, בקבוצה "עיצוב"',
    Mo.CMDS['עצב'].det.length===46&&Mo.CMDS['ודא עיצוב'].det.length===46&&(Mo.menu().find(([gr])=>gr==='עיצוב')||[0,[]])[1].join()==='עצב,ודא עיצוב');
  const same=['עצב תוכנה /טורים:3','עצב "עבודה" /מראה:כהה /צבע:"טקסט משני"=#112233',
    'עצב "עבודה\\לקוחות\\פעילים" /עיצוב:"מנהל קבצים עם צלמיות" /סגנון:רשת /סביב-הצלמית:"ריבוע צבעוני" /שורות-לשם:2',
    'עצב "עבודה\\לקוחות" /סגנון:"" /טורים:"" /עיצוב:""','עצב "עבודה\\לקוחות" /כתב-שם:16,מודגש,"דוד.ttf" /תווית:"רשימה ידנית"="סביבת עבודה"',
    'ודא עיצוב "עבודה\\לקוחות" /טורים:3 /סגנון:רשת','ודא עיצוב תוכנה /מראה:בהיר','ודא עיצוב "עבודה\\לקוחות" /תווית:"פקודה"=""'];
  const diff=same.filter(l=>{const m=Mo.fromLine(l);return !m||Mo.toLine(m,'he')!==l;});
  say('עצב: שורה נקראת ונכתבת בדיוק כמו שהיא ('+same.length+' שורות'+(diff.length?'; לא: '+diff.join(' ¦ '):'')+')',!diff.length);
  const raw=['עצב "עבודה\\לקוחות" /לא-קיים:3','עצב "עבודה\\לקוחות" /טורים:3 /טורים:4','עצב "עבודה\\לקוחות" /מראה:כהה','ודא עיצוב "עבודה\\לקוחות" /טורים:""'];
  say('עצב: מתג שהתוכנה לא מסרה, מתג פעמיים, צבעים ברשימה, "" ב"ודא עיצוב" — השורה נשארת כמו שהיא',raw.every(l=>Mo.fromLine(l)===null));
  say('עצב: "VERIFY עיצוב" נקרא; בלי יעד או בלי פרטים — נאמר מה חסר',Mo.fromLine('VERIFY עיצוב תוכנה /מראה:בהיר').cmd==='ודא עיצוב'
    &&Mo.missing(Mo.fromLine('עצב /טורים:3')).join()==='את מה מעצבים'&&Mo.missing(Mo.fromLine('עצב תוכנה')).join()==='פרטים (מה לקבוע)');
  a.set('עצב תוכנה /טורים:3\n');
  say('עצב: שורה סגורה — "כל התוכנה" והפרט: '+a.row(0).textContent.replace(/\s+/g,' ').trim(),a.row(0).textContent.includes('כל התוכנה')&&a.row(0).textContent.includes('טורים 3'));
  a.hit(a.row(0));a.hit(a.chip(0,'פרטים'));
  const secs=a.its().filter(b=>b.hasAttribute('aria-expanded'));
  say('עצב: גיליון הפרטים — שישה חלקים סגורים, וליד "טורים, ריווח וצלמית" — כמה נבחרו',secs.length===6&&secs.every(b=>b.getAttribute('aria-expanded')==='false')
    &&a.it('טורים, ריווח וצלמית').textContent.includes('1 ✓')&&a.its().length===6);
  a.hit(a.it('הפריט'));
  say('עצב: "הפריט" נפתח — אחד־עשר פרטים, עם הטווח ליד מספר',a.its().length===17&&a.it('שורות לשם').textContent.includes('1–6'));
  a.hit(a.it('סגנון'));
  say('עצב: בחירה מתוך הערכים שהתוכנה מסרה, ו"כמו שמעליו": '+a.keys().map(k=>k.textContent).join(' | '),a.keys().map(k=>k.textContent).join('|')==='רשת|שורות|'+Mo.RESET);
  a.hit(a.btn('בחירה'));
  say('עצב: פרט בלי ערך — לא נסגר, ונאמר',a.sheetOpen()&&a.toast().includes('חסר ערך: סגנון'));
  a.hit(a.key('רשת'));a.hit(a.btn('בחירה'));
  say('עצב: נכתב — '+a.get(),a.get()==='עצב תוכנה /טורים:3 /סגנון:רשת');
  a.hit(a.row(0));a.hit(a.chip(0,'פרטים'));a.hit(a.it('טורים, ריווח וצלמית'));
  const f1=a.D.querySelector('#sheetIn input.fi');a.type(f1,'9');a.hit(a.btn('בחירה'));
  say('עצב: מחוץ לטווח שהתוכנה מסרה — לא נסגר, ונאמר ('+a.toast()+')',a.sheetOpen()&&a.toast().includes('מחוץ לטווח')&&a.toast().includes('1–8'));
  a.hit(a.key(Mo.RESET));a.hit(a.btn('בחירה'));
  say('עצב: "כמו שמעליו" נכתב כ־"" — '+a.get(),a.get()==='עצב תוכנה /טורים:"" /סגנון:רשת');
  a.hit(a.row(0));a.hit(a.chip(0,'פרטים'));a.hit(a.it('הכתב'));a.hit(a.it('כתב שם'));
  a.type(a.D.querySelector('#sheetIn input.fi'),'16');
  say('עצב: לכתב — המשקלים והגופנים שהתוכנה מסרה, בנגיעה',['דק','רגיל','בינוני','מודגש','מערכת','דוד.ttf'].every(h=>!!a.key(h)));
  a.hit(a.key('מודגש'));a.hit(a.key('דוד.ttf'));
  a.hit(a.it('הנוסח של תווית הסוג'));a.hit(a.it('תווית: פקודה'));a.type(a.D.querySelector('#sheetIn input.fi'),'סקריפט');
  a.hit(a.it('צבעים'));a.hit(a.it('צבע: טקסט —')||a.its().find(b=>b.textContent.replace('✓','').trim()==='צבע: טקסט'));a.type(a.D.querySelector('#sheetIn input.fi'),'#112233');
  a.hit(a.btn('בחירה'));
  say('עצב: כתב, נוסח וצבע — '+a.get(),a.get()==='עצב תוכנה /טורים:"" /סגנון:רשת /כתב-שם:16,מודגש,"דוד.ttf" /תווית:"פקודה"="סקריפט" /צבע:"טקסט"=#112233');
  a.hit(a.row(0));a.hit(a.chip(0,'כל התוכנה'));
  say('עצב: הבורר — מה מעצבים: '+a.its().map(b=>b.textContent.replace('›','').trim()).join(' | '),a.its().length===3&&!!a.it('כל התוכנה')&&!!a.it('אופן')&&!!a.it('רשימה'));
  a.hit(a.it('רשימה'));await wait(80);
  say('עצב: רשימה — הדרך מתחילה באופן; הצבע ירד, ונאמר ('+a.toast()+')',a.get()==='עצב "עבודה\\לקוחות\\פעילים" /טורים:"" /סגנון:רשת /כתב-שם:16,מודגש,"דוד.ttf" /תווית:"פקודה"="סקריפט"'
    &&a.toast().includes('ירד')&&a.toast().includes('צבע: טקסט'));
  a.hit(a.row(0));a.hit(a.chip(0,'פרטים'));
  say('עצב: לרשימה — בלי החלק של הצבעים',a.its().filter(b=>b.hasAttribute('aria-expanded')).length===5&&!a.it('צבעים'));
  a.shade();
  a.hit(a.row(0));a.hit([...a.row(0).querySelectorAll('.chip.val')][0]);a.hit(a.it('אופן'));a.hit(a.it('בדיקות מערכת'));
  say('עצב: אופן — '+a.get().split(' /')[0],a.get().startsWith('עצב "בדיקות מערכת" /טורים:""'));
  a.set('ודא עיצוב "עבודה" /טורים:3\n');a.hit(a.row(0));a.hit(a.chip(0,'פרטים'));a.hit(a.it('טורים, ריווח וצלמית'));
  const noReset=!a.key(Mo.RESET);a.hit(a.it('הנוסח של תווית הסוג'));a.hit(a.it('תווית: פקודה'));
  say('ודא עיצוב: "כמו שמעליו" — רק בנוסח של תווית ("" = כמו במילון)',noReset&&!!a.key(Mo.RESET));
  a.shade();
  a.set('\n');a.hit(a.row(0));a.hit(a.chip(0,'רשימת פקודות'));
  say('עצב: בעץ הפקודות — הקבוצה "עיצוב"',!!a.it('עיצוב'));
  a.hit(a.it('עיצוב'));a.hit(a.it('עצב'));
  const t21=a.get(),miss21=(a.D.querySelector('#ed .miss')||{textContent:''}).textContent;
  say('עצב: שורה חדשה — נאמר מה חסר ('+miss21+')',t21==='עצב'&&miss21.includes('מה מעצבים')&&miss21.includes('פרטים'));
  say('עצב: בלי שגיאות בעמוד'+(a.er.length?': '+a.er.join(' ¦ '):''),!a.er.length);
  a.W.close();
  // תוכנה שאינה מכירה את "עצב" (אין design, ואין "design" במה שהיא יודעת): לא מוצע, ושורה של "עצב" נשארת כמו שהיא
  const b=page({__noDesign:true}),l0='עצב תוכנה /טורים:3';
  b.set(l0+'\n');b.hit(b.row(0));
  const rawRow=!b.chip(0,'פרטים');b.D.querySelector('#ed').dispatchEvent(new b.W.MouseEvent('click',{bubbles:true}));
  b.set(l0+'\n\n');b.hit(b.row(1));b.hit(b.chip(1,'רשימת פקודות'));
  say('תוכנה שאינה מכירה עיצוב: אין פרטים, אין בתפריט, לא מוצעת בהקלדה, והשורה נשארת כמו שהיא',b.Mo.compat.design===false&&b.Mo.CMDS['עצב'].det.length===0
    &&b.Mo.CMDS['עצב'].auto===true&&!b.it('עיצוב')&&b.Mo.fromLine(l0)===null&&rawRow&&b.get().split('\n')[0]===l0&&!b.er.length);
  b.W.close();
  // תוכנה כמו מ0.47: מכירה את "עצב", בלי "הפריט", בלי תווית הסוג ובלי נוסח — מוצע רק מה שהיא מסרה
  const c=page({__m047:true});
  c.set('עצב תוכנה /טורים:3\n');c.hit(c.row(0));c.hit(c.chip(0,'פרטים'));
  say('תוכנה כמו מ0.47: רק מה שהיא מסרה ('+c.Mo.CMDS['עצב'].det.length+' פרטים, '+c.its().length+' חלקים); מתג שאינה מכירה — השורה נשארת כמו שהיא',
    c.Mo.CMDS['עצב'].det.length===27&&c.its().length===4&&!c.it('הפריט')&&!c.it('הנוסח')&&c.Mo.fromLine('עצב תוכנה /סגנון:רשת')===null&&!c.er.length);
  c.W.close();
})().catch(e=>out.push('❌ חריגה (עצב): '+e.message)).then(()=>{
console.log(out.join('\n'));if(errs.length)console.log('שגיאות עמוד:\n'+errs.join('\n'));
});
