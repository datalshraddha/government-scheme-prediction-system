const $ = s => document.querySelector(s);
const esc = t => String(t).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let DATA = [], FILTER = 'All';

fetch('/api/stats').then(r => r.json()).then(d => { $('#st-schemes').textContent = d.schemes; $('#st-searches').textContent = d.searches; }).catch(() => {});

$('#income').addEventListener('input', e => {
  const v = Number(e.target.value);
  $('#income-hint').textContent = v >= 100000 ? '= ' + (v / 100000).toFixed(2).replace(/\.?0+$/, '') + ' lakh per year' : 'Total yearly income of your family';
});

$('#form').addEventListener('submit', async e => {
  e.preventDefault();
  const err = $('#err'), btn = $('#go');
  err.hidden = true;
  const f = new FormData(e.target);
  for (const k of ['age', 'gender', 'caste', 'income', 'occupation']) {
    if (!f.get(k)) { err.textContent = 'Please fill in every field.'; err.hidden = false; return; }
  }
  btn.disabled = true; btn.textContent = 'Checking schemes...';
  try {
    const res = await fetch('/api/match?' + new URLSearchParams(f));
    const data = await res.json();
    if (data.error) throw new Error(data.error);
    DATA = data; FILTER = 'All'; render();
    $('#results').hidden = false;
    $('#results').scrollIntoView({ behavior: 'smooth' });
    fetch('/api/stats').then(r => r.json()).then(d => { $('#st-searches').textContent = d.searches; });
  } catch (ex) {
    err.textContent = ex.message || 'Could not reach the server. Is the Java app running?'; err.hidden = false;
  } finally { btn.disabled = false; btn.textContent = 'Show my schemes'; }
});

function render() {
  const yes = DATA.filter(d => d.eligible).length, near = DATA.length - yes;
  $('#sum-title').textContent = yes ? `You qualify for ${yes} scheme${yes > 1 ? 's' : ''}` + (near ? ` and are close to ${near} more` : '') : (near ? `No exact match, but you are close to ${near}` : 'No matching schemes found');
  const cats = ['All', ...new Set(DATA.map(d => d.category))];
  $('#chips').innerHTML = cats.map(c => `<button class="${c === FILTER ? 'on' : ''}" data-c="${esc(c)}">${esc(c)}</button>`).join('');
  $('#chips').querySelectorAll('button').forEach(b => b.onclick = () => { FILTER = b.dataset.c; render(); });
  const rows = DATA.filter(d => FILTER === 'All' || d.category === FILTER);
  $('#list').innerHTML = rows.length ? rows.map(card).join('') :
    '<div class="empty">Nothing here yet. Try changing the income or occupation and search again.</div>';
  requestAnimationFrame(() => requestAnimationFrame(() =>
    document.querySelectorAll('.ring').forEach(r => r.style.setProperty('--p', r.dataset.p))));
}

function card(d) {
  const why = d.eligible ? d.reasons.map(r => `<li>${esc(r)}</li>`).join('') : d.gaps.map(g => `<li class="gap">${esc(g)}</li>`).join('');
  return `<article class="row ${d.eligible ? '' : 'almost'}">
    <div class="ring" data-p="${d.score}"><b>${d.score}</b></div>
    <div>
      <div class="meta"><span class="tag">${esc(d.category)}</span><span class="lvl">${esc(d.level)} scheme</span>${d.eligible ? '' : '<span class="status">Almost eligible</span>'}</div>
      <h3>${esc(d.name)}</h3>
      <p>${esc(d.benefits)}</p>
      <ul class="why">${why}</ul>
      <details><summary>Documents you may need</summary><p>${esc(d.documents)}</p></details>
    </div>
    <a class="apply" href="${esc(d.url)}" target="_blank" rel="noopener">Official website</a>
  </article>`;
}
