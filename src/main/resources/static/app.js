const $ = (selector) => document.querySelector(selector);

fetch('/api/v1/system/status').then(r => r.json()).then(status => {
  $('#system-status').textContent = `Jev: ${status.jev} · 生成器: ${status.generator}`;
}).catch(() => $('#system-status').textContent = '服务状态不可用');

$('#optimize-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector('button');
  button.disabled = true;
  button.firstChild.textContent = '正在评估与生成… ';
  const data = new FormData(event.currentTarget);
  const parseLines = (name) => String(data.get(name) || '').split('\n').map(v => v.trim()).filter(Boolean);
  const payload = {
    channel: data.get('channel'), productName: data.get('productName'), market: data.get('market'),
    language: data.get('language'), audience: data.get('audience'), goal: data.get('goal'),
    currentContent: data.get('currentContent'), brandRules: parseLines('brandRules'), competitors: [],
    keywords: parseLines('keywords').map(line => {
      const [keyword, volume, difficulty, rank] = line.split(',').map(v => v.trim());
      return { keyword, monthlyVolume: numberOrNull(volume), difficulty: numberOrNull(difficulty), currentRank: numberOrNull(rank) };
    })
  };
  try {
    const response = await fetch('/api/v1/optimizations', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
    if (!response.ok) { const problem = await response.json(); throw new Error(problem.detail || '请求失败'); }
    render(await response.json());
  } catch (error) { alert(error.message); }
  finally { button.disabled = false; button.firstChild.textContent = '运行 Jev 决策与内容优化 '; }
});

function numberOrNull(value) { return value === '' || value == null ? null : Number(value); }
function pct(value) { return `${Math.round(value * 100)}%`; }

function render(report) {
  $('#empty-state').classList.add('hidden'); $('#result').classList.remove('hidden');
  const d = report.decision, c = report.content;
  $('#result-status').textContent = `${report.status} · ${d.source}`;
  $('#opportunity').textContent = d.opportunityScore; $('#readiness').textContent = d.readinessScore;
  $('#confidence').textContent = pct(d.confidence); $('#brand-safe').textContent = pct(d.brandSafeProbability);
  $('#strategy').textContent = `策略：${d.strategy}`; $('#content-title').textContent = c.title;
  $('#meta').textContent = c.metaDescription || c.shortDescription || '';
  $('#descriptions').innerHTML = `<p>${escapeHtml(c.longDescription || '').replaceAll('\n','<br>')}</p>`;
  $('#outline').innerHTML = (c.outline || []).map(item => `<li>${escapeHtml(item)}</li>`).join('');
  $('#keyword-list').innerHTML = Object.values(d.keywordVerdicts).map(k =>
    `<span class="chip">${escapeHtml(k.keyword)} · ${k.intent} · ${k.fitScore}</span>`).join('');
  $('#recommendations').innerHTML = report.recommendations.map(item => `<div class="notice">${escapeHtml(item)}</div>`).join('');
  const review = report.contentReview;
  $('#content-review').textContent = `生成后 Jev 路由：${review.publishDecision} · 意图对齐 ${review.intentAlignmentScore} · 事实落地 ${pct(review.groundedProbability)} · 关键词自然度 ${pct(review.naturalKeywordProbability)}`;
  $('#experiment').textContent = `实验指标：${report.experiment.primaryMetric}；建议至少运行 ${report.experiment.minimumDays} 天。`;
  $('#result').scrollIntoView({ behavior:'smooth', block:'start' });
}
function escapeHtml(value) { const div=document.createElement('div'); div.textContent=value; return div.innerHTML; }
