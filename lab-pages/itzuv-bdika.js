// בדיקות העמוד של תוסף העיצוב (הלוח) — בדפדפן מדומה, עם הגשר לדוגמה שבעמוד. להריץ:
// NODE_PATH=/tmp/orech/node_modules node lab-pages/itzuv-bdika.js source/assets/plugins/itzuv.html
const {JSDOM,VirtualConsole}=require('jsdom');const fs=require('fs');
const html=fs.readFileSync(process.argv[2],'utf8');
const errs=[];
const vc=new VirtualConsole().on('jsdomError',e=>errs.push(String(e.detail||e.message||e).slice(0,300))).on('error',e=>errs.push(String(e).slice(0,300)));
const out=[];const say=(n,ok)=>out.push((ok?'✅ ':'❌ ')+n);
function page(start){
  const dom=new JSDOM(html,{runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:vc,beforeParse(w){if(start)w.__start=start;}});
  return dom.window;
}
const run=async()=>{
  let w=page(null),d=w.document;
  const $=id=>d.getElementById(id);const click=el=>el.dispatchEvent(new w.MouseEvent('click',{bubbles:true}));
  const chips=sel=>[...d.querySelectorAll(sel+' .chip')];
  const chip=(sel,t)=>chips(sel).find(b=>b.textContent.trim().startsWith(t));
  const openM=m=>click(d.querySelector('#mbar button[data-m="'+m+'"]'));
  const items=()=>[...$('menu').querySelectorAll('button')];
  const pick=(m,t)=>{openM(m);const b=items().find(x=>x.textContent.replace('✓','').trim()===t);if(!b)throw new Error('אין פריט '+t);if(b.disabled){openM(m);return false;}click(b);return true;};
  const cmd=()=>$('cmd').textContent;
  const setNum=(el,v)=>{el.value=String(v);el.dispatchEvent(new w.Event('change',{bubbles:true}));};
  try{
    say('ארבעה תפריטים, בסדר של חלונות',[...d.querySelectorAll('#mbar button[data-m]')].map(b=>b.textContent).join(' ')==='קובץ עריכה תצוגה עזרה');
    say('סרגל הקיצורים: ביטול · חזרה · ברירת מחדל · החלה',[...$('tbar').querySelectorAll('button')].map(b=>b.textContent).join(' | ')==='↶ ביטול | ↷ חזרה | ברירת מחדל | ✓ החלה');
    say('מתחילים בכל התוכנה (בלי רשימה מהמסך)',chip('#who','כל התוכנה').classList.contains('on')&&!chip('#who','הרשימה'));
    say('בלי שינוי — אין פקודה, "החלה" כבויה',cmd().startsWith('אין שינויים')&&$('apply').disabled);
    say('צבעים — לכל התוכנה: 18, בחמש קבוצות',d.querySelectorAll('#main details').length===5&&d.querySelectorAll('#main .cl').length===18);
    // טורים — במחוון / בשדה
    const colsNum=d.querySelectorAll('#main .card')[1].querySelector('.num');
    setNum(colsNum,4);
    say('טורים 4 ← הפקודה: '+cmd(),cmd()==='עצב תוכנה /טורים:4');
    setNum(d.querySelectorAll('#main .card')[1].querySelector('.num'),9);
    say('טורים 9 — מחוץ לטווח: לא נכנס',cmd()==='עצב תוכנה /טורים:4');
    // עיצוב מוכן
    click(chip('#main','מילים וצבע'));
    say('עיצוב מוכן ← '+cmd(),cmd()==='עצב תוכנה /עיצוב:"מילים וצבע" /טורים:4');
    say('התצוגה המקדימה — רשת, 4 טורים',$('pv').querySelector('.items').style.gridTemplateColumns.startsWith('repeat(4'));
    // כתב
    const tx=[...d.querySelectorAll('#main .tx')];
    say('חמישה סוגי כתב — החמישי: תווית הסוג',tx.length===5&&tx[3].textContent.includes('כותרת קבוצה')&&tx[4].textContent.includes('תווית הסוג'));
    click([...tx[1].querySelectorAll('.seg button')].find(b=>b.textContent==='מודגש'));
    setNum(d.querySelectorAll('#main .tx')[1].querySelector('.num'),18);
    say('שם: 18, מודגש ← '+cmd(),cmd().includes('/כתב-שם:18,מודגש'));
    // תווית הסוג — קטנה יותר
    setNum(d.querySelectorAll('#main .tx')[4].querySelector('.num'),8);
    say('תווית הסוג 8 ← '+cmd(),cmd().includes('/כתב-תווית:8'));
    const tg=$('pv').querySelector('.tg')||[...$('pv').querySelectorAll('.ic')].find(e=>e.textContent==='ידנית');
    say('התצוגה המקדימה — התווית בגודל 8',!!tg&&tg.style.fontSize==='8px');
    // הנוסח של התוויות
    const wrow=[...d.querySelectorAll('#main .row')].find(r=>r.textContent.startsWith('רשימה ידנית'));
    setNum(wrow.querySelector('input'),'סביבת עבודה');
    say('נוסח לתווית ← '+cmd(),cmd().includes('/תווית:"רשימה ידנית"="סביבת עבודה"'));
    say('התצוגה המקדימה — "סביבת עבודה" בפס',[...$('pv').querySelectorAll('.tg')].some(e=>e.textContent==='סביבת עבודה'));
    setNum([...d.querySelectorAll('#main .row')].find(r=>r.textContent.startsWith('פקודה')).querySelector('input'),'א · ב');
    say('נוסח עם "·" — לא נכנס',!cmd().includes('/תווית:"פקודה"'));
    // צבעים
    const rows=()=>[...d.querySelectorAll('#main .cl')];
    const hx=rows().find(r=>r.textContent.includes('טקסט משני')).querySelector('.hex');
    setNum(hx,'#123456');
    say('צבע בשדה ← '+cmd(),cmd().includes('/צבע:"טקסט משני"=#123456'));
    setNum(rows().find(r=>r.textContent.includes('רקע רך')).querySelector('.hex'),'כחול');
    say('צבע לא כתוב נכון — לא נכנס',!cmd().includes('רקע רך'));
    click([...d.querySelectorAll('#main .seg button')].find(b=>b.textContent==='כהה'));
    say('מראה כהה ← '+cmd(),cmd().includes('/מראה:כהה'));
    say('התצוגה המקדימה — רקע כהה',$('pv').style.background.replace(/\s/g,'')==='rgb(32,33,36)');
    // ביטול / חזרה
    const before=cmd();
    pick('edit','ביטול');say('ביטול מחזיר צעד',cmd()!==before&&!cmd().includes('/מראה:'));
    pick('edit','חזרה');say('חזרה',cmd()===before);
    // החלה
    click($('apply'));
    const sh=w.__shaped[w.__shaped.length-1];
    say('החלה — שורה אחת של "עצב" לגשר: '+sh,sh===before);
    say('אחרי החלה — אין שינויים, ומה שנקבע נקרא מחדש',cmd().startsWith('אין שינויים')&&chip('#main','מילים וצבע').classList.contains('on'));
    // ↺ — חוזר לרשת
    const re=d.querySelectorAll('#main .card')[1].querySelector('.re');
    click(re);say('↺ על טורים ← '+cmd(),cmd()==='עצב תוכנה /טורים:""');
    pick('edit','ביטול כל השינויים');say('ביטול כל השינויים',cmd().startsWith('אין שינויים'));
    // ביטול ההחלה האחרונה
    say('ביטול ההחלה האחרונה',pick('edit','ביטול ההחלה האחרונה')&&w.__shaped[w.__shaped.length-1].includes('/עיצוב:""')&&w.__shaped[w.__shaped.length-1].includes('/מראה:""')&&chip('#main','כמו').classList.contains('on'));
    // אופן
    click(chip('#who','האופן'));
    say('האופן — "כמו כל התוכנה"',chip('#main','כמו כל התוכנה')!=null&&$('fn').textContent.includes('האופן'));
    setNum(d.querySelectorAll('#main .card')[1].querySelectorAll('.num')[1],12);
    say('ריווח באופן ← '+cmd(),cmd()==='עצב "עבודה" /ריווח:12');
    // עזיבה עם שינויים — שואלים
    click(chip('#who','כל התוכנה'));
    const ask=d.querySelector('.sheet');
    say('יש שינויים שלא הוחלו — שואלים לפני שעוברים',!!ask&&ask.textContent.includes('לא הוחלו'));
    click([...ask.querySelectorAll('button')].find(b=>b.textContent==='לעזוב אותם'));
    say('לעזוב אותם — עוברים לכל התוכנה',chip('#who','כל התוכנה').classList.contains('on')&&cmd().startsWith('אין שינויים'));
    // רשימה מהבורר
    click(chip('#who','בחירת רשימה'));
    await new Promise(r=>setTimeout(r,60));
    say('בחירת רשימה — הלוח על הרשימה: '+$('fn').textContent,$('fn').textContent.includes('פעילים'));
    say('ברשימה — בלי צבעים',d.querySelectorAll('#main .cl').length===0&&d.querySelector('#main').textContent.includes('לכל התוכנה או לאופן'));
    say('ברשימה — "כמו הרשימה שמעליה"',!!chip('#main','כמו הרשימה שמעליה'));
    setNum(d.querySelectorAll('#main .card')[1].querySelector('.num'),2);
    say('רשימה — הדרך מתחילה באופן ← '+cmd(),cmd()==='עצב "עבודה\\לקוחות\\פעילים" /טורים:2');
    // כתב שחלק ממנו חוזר לרשת
    const t2=d.querySelectorAll('#main .tx')[2];
    setNum(t2.querySelector('.num'),13);click(d.querySelector('#apply'));
    click(d.querySelectorAll('#main .tx')[2].querySelector('.re'));
    say('כתב חוזר לרשת ← '+cmd(),cmd()==='עצב "עבודה\\לקוחות\\פעילים" /כתב-נתונים:""');
    // שגיאה מהתוכנה — נאמרת, ולא נשמר
    pick('edit','ביטול כל השינויים');
    w.__shaped.length=0;
    setNum(d.querySelectorAll('#main .card')[1].querySelector('.num'),8);
    const l8=cmd();
    say('סירוב מהתוכנה — השינוי נשאר, ולא "הוחל"',l8.includes('/טורים:8'));
    // תצוגה
    say('תצוגה — הסתרת התצוגה המקדימה',pick('view','תצוגה מקדימה')&&$('pvCard').hidden);
    pick('view','תצוגה מקדימה');
    say('עזרה — על הלוח',pick('help','על הלוח')&&!!d.querySelector('.sheet h3'));
    // מהמסך של רשימה
    w=page('עבודה\\ספרים\\קודש');d=w.document;
    say('נפתח מרשימה — הלוח עליה, והבורר מציע אותה',d.getElementById('fn').textContent.includes('קודש')&&[...d.querySelectorAll('#who .chip')].some(b=>b.textContent.includes('"קודש"')&&b.classList.contains('on')));
    say('הרשימה בחרה "מנהל קבצים עם צלמיות" וטורים 4 — כך נראה',[...d.querySelectorAll('#main .chip.on')].some(b=>b.textContent==='מנהל קבצים עם צלמיות')&&d.querySelectorAll('#main .card')[1].querySelector('.num').value==='4');
    // ---------- הניסוי (מבנה 251) — רק בתצוגה ----------
    w=page(null);d=w.document;
    const tc=()=>$('tryCard'),pv=()=>$('pv');
    const tchip=t=>[...tc().querySelectorAll('.chip')].find(b=>b.textContent===t);
    const shaped0=w.__shaped.length;
    const clean=()=>cmd().startsWith('אין שינויים')&&$('apply').disabled&&w.__shaped.length===shaped0;
    say('הניסוי — כרטיס משלו, ונאמר שהוא רק בתצוגה',!tc().hidden&&tc().textContent.includes('רק בתצוגה')&&tc().textContent.includes('לא נכנס לפקודה'));
    say('עשרה שילובים מוכנים, בשמות',tc().querySelectorAll('#tryCombos .chip').length===10&&!!tchip('גלריה')&&!!tchip('כרטיסי קטגוריות'));
    say('בלי ניסוי — התצוגה כמו קודם (קבוצה אחת, בלי "ניסוי")',pv().querySelectorAll('.items').length===1&&!$('pvNote').textContent.includes('ניסוי'));
    click(tchip('גלריה'));
    const gi=pv().querySelector('.items');
    say('גלריה — 3 טורים, צמוד, תמונות בלי שמות',gi.style.gridTemplateColumns.startsWith('repeat(3')&&gi.style.gap==='0px'&&pv().querySelectorAll('.th').length>=5&&!pv().querySelector('.nm2'));
    say('גלריה — מסומנת, וב"תצוגה מקדימה" כתוב "ניסוי"',tchip('גלריה').classList.contains('on')&&$('pvNote').textContent.includes('ניסוי'));
    say('הניסוי לא נכנס לפקודה ולא ל"החלה", ולא נשלח לתוכנה',clean());
    click(tchip('עיגולים צבעוניים'));
    say('עיגולים צבעוניים — עיגול סביב הצלמית, 4 טורים, בלי פס הסוג',[...pv().querySelectorAll('.ic')].some(e=>e.style.borderRadius==='50%')&&pv().querySelector('.items').style.gridTemplateColumns.startsWith('repeat(4')&&!pv().querySelector('.tg'));
    click(tchip('כרטיסי קטגוריות'));
    say('כרטיסי קטגוריות — שורת כרטיסים לצד, צלמית לצד השם, ⋮ לכותרת',pv().querySelectorAll('.car .cc').length===4&&[...pv().querySelectorAll('.it.g')].every(e=>e.style.flexDirection==='row')&&!!pv().querySelector('.grp.gm .gdots'));
    click(tchip('ניתוח מקום'));
    say('ניתוח מקום — כרטיס רחב, ופס ואחוז בקצה כל שורה',!!pv().querySelector('.wide .ring')&&pv().querySelectorAll('.bw').length===6&&pv().querySelector('.bp').textContent==='38%');
    click(tchip('שני חלקים'));
    const more=()=>pv().querySelector('.more');
    say('שני חלקים — שתי קבוצות בשני סגנונות, לשוניות, ו"עוד" בקבוצה המקופלת',pv().querySelectorAll('.items').length===2&&pv().querySelectorAll('.items')[0].style.gridTemplateColumns.startsWith('repeat(2')&&!!pv().querySelector('.tabs .tab.on')&&!!more()&&more().textContent.startsWith('עוד'));
    const n0=pv().querySelectorAll('.items')[1].querySelectorAll('.it').length;
    click(more());
    say('"עוד" — פותח את הקבוצה ('+n0+' ← '+pv().querySelectorAll('.items')[1].querySelectorAll('.it').length+')',pv().querySelectorAll('.items')[1].querySelectorAll('.it').length>n0&&more().textContent==='פחות');
    const clr=()=>[...tc().querySelectorAll('button')].find(b=>b.textContent==='ניקוי');
    click(clr());
    say('ניקוי — התצוגה חוזרת, בלי "ניסוי"',pv().querySelectorAll('.items').length===1&&!$('pvNote').textContent.includes('ניסוי')&&clr().disabled&&!tc().querySelector('#tryCombos .chip.on'));
    // מתגים אחד־אחד
    click(tchip('מוסתר — חיוור'));
    say('מוסתר — פריט ששמו מתחיל בנקודה, חיוור',[...pv().querySelectorAll('.it')].some(e=>e.style.opacity==='0.45'&&e.textContent.includes('.גיבוי')));
    click(tchip('סמל קטן על הצלמית'));
    say('סמל קטן על הצלמית',pv().querySelectorAll('.bd').length>=1);
    click(tchip('נתונים בצד'));
    say('נתונים בצד השם',pv().querySelectorAll('.tt.side').length>=6);
    click(tchip('ריבוע חי'));
    say('רקע לצלמית — ריבוע חי',[...pv().querySelectorAll('.ic')].some(e=>e.style.borderRadius==='22%'));
    say('גם המתגים — לא בפקודה',clean());
    // שמירה בשם — בלוח בלבד
    const saveBtn=()=>[...tc().querySelectorAll('button')].find(b=>b.textContent==='שמירה בשם…');
    click(saveBtn());
    let sh2=d.querySelector('.sheet');
    say('שמירה בשם — נאמר שזה בלוח בלבד',!!sh2&&sh2.textContent.includes('בלוח בלבד'));
    const inp=sh2.querySelector('input'),okb=()=>[...sh2.querySelectorAll('button')].find(b=>b.classList.contains('main'));
    inp.value='גלריה';click(okb());
    say('שם של שילוב מוכן — לא',!!d.querySelector('.sheet')&&sh2.querySelector('.err').textContent.includes('שילוב מוכן'));
    inp.value='';click(okb());say('בלי שם — לא',sh2.querySelector('.err').textContent==='צריך שם');
    inp.value='שלי';click(okb());
    say('נשמר — "שלי" בין השילובים, ומסומן',!d.querySelector('.sheet')&&!!tc().querySelector('#trySaved')&&tchip('שלי').classList.contains('on'));
    click(tchip('גלריה'));click(tchip('שלי'));
    say('בחירה ב"שלי" — מחזירה את השילוב',[...pv().querySelectorAll('.ic')].some(e=>e.style.borderRadius==='22%')&&pv().querySelectorAll('.bd').length>=1&&tchip('שלי').classList.contains('on'));
    click(tchip('גלריה'));click(saveBtn());sh2=d.querySelector('.sheet');sh2.querySelector('input').value='שלי';click([...sh2.querySelectorAll('button')].find(b=>b.classList.contains('main')));
    say('שם שכבר יש — שואלים לפני שמחליפים',!!d.querySelector('.sheet')&&sh2.querySelector('.err').textContent.includes('כבר יש')&&[...sh2.querySelectorAll('button')].some(b=>b.textContent==='להחליף'));
    click([...sh2.querySelectorAll('button')].find(b=>b.textContent==='ביטול'));
    click(tchip('שלי'));
    click([...tc().querySelectorAll('button')].find(b=>b.textContent==='מחיקת "שלי"'));
    sh2=d.querySelector('.sheet');
    say('מחיקה — שואלים',!!sh2&&sh2.textContent.includes('למחוק'));
    click([...sh2.querySelectorAll('button')].find(b=>b.textContent==='מחיקה'));
    say('נמחק — "שלי" כבר לא בין השילובים',!tc().querySelector('#trySaved'));
    say('כל הניסוי — בלי פקודה ובלי "החלה"',clean());
    say('תצוגה — הסתרת הניסוי',pick('view','ניסוי רעיונות')&&tc().hidden);
    // ---------- הפריט — קבוצה א, באמת בפקודה (מבנה 255) ----------
    w=page(null);d=w.document;
    const sc=()=>$('shapeCard'),srow=t=>[...sc().querySelectorAll('.row')].find(r=>r.querySelector('.lb').textContent===t);
    const sbtn=(t,v)=>[...srow(t).querySelectorAll('.seg button')].find(b=>b.textContent===v);
    say('כרטיס "הפריט" — תשעה, מהתוכנה',!!sc()&&sc().querySelectorAll('.row').length===9&&!!srow('סביב הצלמית')&&!!srow('שורות לשם'));
    say('מה שנראה מסומן (רשימת קבצים: שורות, ריבוע צבעוני)',sbtn('סגנון','שורות').classList.contains('on')&&sbtn('סביב הצלמית','ריבוע צבעוני').classList.contains('on'));
    click(sbtn('סגנון','רשת'));
    say('סגנון: רשת — עם הטורים, הריווח והצלמית של רשת ← '+cmd(),cmd()==='עצב תוכנה /טורים:3 /ריווח:8 /גודל-צלמית:60 /סגנון:רשת'&&$('pv').querySelectorAll('.it.g').length===6);
    pick('edit','ביטול');
    say('ביטול אחד — מחזיר את כל הארבעה',cmd().startsWith('אין שינויים'));
    pick('edit','חזרה');
    click(sbtn('סביב הצלמית','ריבוע צבעוני'));click(sbtn('סביב הצלמית','מסגרת'));
    click(sbtn('צבע רקע לפריט','כן'));click(sbtn('מקום הנתונים','בצד'));
    say('עוד שלושה ← '+cmd(),cmd()==='עצב תוכנה /טורים:3 /ריווח:8 /גודל-צלמית:60 /סגנון:רשת /סביב-הצלמית:מסגרת /צבע-רקע-לפריט:כן /מקום-הנתונים:בצד');
    say('התצוגה — כרטיסים, נתונים בצד, מסגרת',[...$('pv').querySelectorAll('.it')].filter(e=>e.style.borderRadius==='14px').length>=5&&$('pv').querySelectorAll('.tt.side').length===6
      &&[...$('pv').querySelectorAll('.ic')].some(e=>e.style.border.includes('1px')));
    click(sbtn('סביב הצלמית','ריבוע צבעוני'));
    say('ערך עם רווח — במירכאות ← '+cmd(),cmd().includes('/סביב-הצלמית:"ריבוע צבעוני"'));
    setNum(srow('שורות לשם').querySelector('.num'),1);
    say('שורות לשם 1 ← '+cmd(),cmd().includes('/שורות-לשם:1')&&!$('pv').querySelector('.nm2.ml'));
    setNum(srow('שורות לשם').querySelector('.num'),9);
    say('שורות לשם 9 — מחוץ לטווח: לא נכנס',cmd().includes('/שורות-לשם:1')&&!cmd().includes('/שורות-לשם:9'));
    click(srow('שורות לשם').querySelector('.re'));
    say('↺ על שורות לשם — חוזר למה שהיה (2 שורות)',!cmd().includes('שורות-לשם')&&!!$('pv').querySelector('.nm2.ml'));
    const n255=w.__shaped.length;click($('apply'));
    const sh255=w.__shaped[w.__shaped.length-1];
    say('החלה — הכול בשורה אחת של "עצב": '+sh255,w.__shaped.length===n255+1&&sh255.includes('/סגנון:רשת')&&sh255.includes('/סביב-הצלמית:"ריבוע צבעוני"'));
    say('אחרי החלה — נקרא מחדש מהתוכנה: "נקבע כאן"',cmd().startsWith('אין שינויים')&&sbtn('סגנון','רשת').classList.contains('on')&&srow('סגנון').textContent.includes('נקבע כאן'));
    click(srow('סגנון').querySelector('.re'));
    say('↺ אחרי החלה ← '+cmd(),cmd()==='עצב תוכנה /סגנון:""');
    // ---------- שגיאה בעמוד — נאמרת על המסך (מבנה 252) ----------
    const dom2=new JSDOM(html,{runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:new VirtualConsole(),beforeParse(x){
      x.vfm={info:()=>JSON.stringify({features:'design'}),theme:()=>null,load:()=>null,save:()=>{},design:()=>{throw new Error('תקלה לבדיקה');}};}});
    const ob=dom2.window.document.getElementById('oops');
    say('שגיאה בעמוד — כרטיס אדום בראש המסך, עם ההודעה',!!ob&&ob.textContent.includes('שגיאה בלוח')&&ob.textContent.includes('תקלה לבדיקה')&&dom2.window.document.body.firstChild===ob);
    say('בלי שגיאה — אין כרטיס',!d.getElementById('oops'));
    // ---------- תוכנה שאינה מכירה את תווית הסוג ואת הנוסח (כמו מ0.47) — הלוח עובד (מבנה 253) ----------
    const errs0=errs.length;
    w=new JSDOM(html,{runScripts:'dangerously',pretendToBeVisual:true,virtualConsole:vc,beforeParse(x){x.__old=true;}}).window;d=w.document;
    say('תוכנה ישנה — אין כרטיס אדום, ואין שגיאות',!d.getElementById('oops')&&errs.length===errs0);
    say('תוכנה ישנה — ארבעה סוגי כתב, בלי "תווית הסוג" ובלי נוסח',d.querySelectorAll('#main .tx').length===4&&!d.querySelector('#main').textContent.includes('נוסח התוויות'));
    say('תוכנה ישנה — התצוגה המקדימה מלאה (6 פריטים, והתווית בגודל הרגיל)',$('pv').querySelectorAll('.it').length===6&&[...$('pv').querySelectorAll('.ic')].some(e=>e.textContent==='ידנית'));
    click([...d.querySelectorAll('#tryCombos .chip')].find(b=>b.textContent==='כרטיסי קטגוריות'));
    say('תוכנה ישנה — גם הניסוי עובד',$('pv').querySelectorAll('.car .cc').length===4&&errs.length===errs0);
    setNum(d.querySelectorAll('#main .card')[1].querySelector('.num'),3);
    say('תוכנה ישנה — הפקודה כמו קודם: '+cmd(),cmd()==='עצב תוכנה /טורים:3');
    say('תוכנה ישנה — בלי כרטיס "הפריט"',!d.getElementById('shapeCard'));
  }catch(e){out.push('❌ חריגה: '+e.stack.split('\n').slice(0,3).join(' | '));}
  const bad=errs.filter(e=>!/Not implemented: (window\.scrollTo|navigator\.clipboard)/.test(e));
  if(bad.length)out.push('❌ שגיאות בעמוד: '+bad.join(' || '));
  console.log(out.join('\n'));
  console.log((out.filter(x=>x.startsWith('✅')).length)+' ✅ · '+(out.filter(x=>x.startsWith('❌')).length)+' ❌');
  process.exit(0);
};
run();
