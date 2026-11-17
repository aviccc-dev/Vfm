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
  }catch(e){out.push('❌ חריגה: '+e.stack.split('\n').slice(0,3).join(' | '));}
  const bad=errs.filter(e=>!/Not implemented: (window\.scrollTo|navigator\.clipboard)/.test(e));
  if(bad.length)out.push('❌ שגיאות בעמוד: '+bad.join(' || '));
  console.log(out.join('\n'));
  console.log((out.filter(x=>x.startsWith('✅')).length)+' ✅ · '+(out.filter(x=>x.startsWith('❌')).length)+' ❌');
  process.exit(0);
};
run();
