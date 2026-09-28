const API = '/api';
const LEVELS = {1: 'L1 - Dept Officer', 2: 'L2 - Senior Supervisor', 3: 'L3 - Director'};
let departments = [];
let grievances = [];

function esc(s) { return String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
function $(id) { return document.getElementById(id); }
function toast(msg, ok = true) {
  const t = $('toast'); t.textContent = msg; t.className = ok ? 'ok' : 'err'; t.style.display = 'block';
  setTimeout(() => t.style.display = 'none', 3000);
}
async function api(path, method = 'GET', body) {
  const res = await fetch(API + path, {
    method, headers: {'Content-Type': 'application/json'}, body: body ? JSON.stringify(body) : undefined
  });
  if (!res.ok) {
    let msg = 'Request failed (' + res.status + ')';
    try { msg = (await res.json()).message || msg; } catch (e) {}
    throw new Error(msg);
  }
  return res.status === 204 ? null : res.json();
}
function openModal(id) { $(id).classList.add('open'); }
function closeModal(id) { $(id).classList.remove('open'); }
function fmt(d) { return d ? new Date(d).toLocaleString() : '-'; }

document.querySelectorAll('.tab').forEach(b => b.onclick = () => {
  document.querySelectorAll('.tab').forEach(x => x.classList.remove('active'));
  document.querySelectorAll('.panel').forEach(x => x.classList.remove('active'));
  b.classList.add('active'); $(b.dataset.tab).classList.add('active');
});

// ---------- departments ----------
async function loadDepartments() {
  try {
    departments = await api('/departments');
    $('departmentBody').innerHTML = departments.map(d => `<tr>
      <td>${d.id}</td><td>${esc(d.name)}</td><td>${esc(d.headOfficer)}</td><td>${esc(d.email)}</td>
      <td><div class="act"><button onclick="openDepartmentForm(${d.id})">Edit</button>
      <button class="danger" onclick="deleteDepartment(${d.id})">Delete</button></div></td></tr>`).join('')
      || '<tr><td colspan="5">No departments yet. Add one first.</td></tr>';
    $('gDept').innerHTML = '<option value="">-- none --</option>' +
      departments.map(d => `<option value="${d.id}">${esc(d.name)}</option>`).join('');
  } catch (e) { toast(e.message, false); }
}
function openDepartmentForm(id) {
  const d = departments.find(x => x.id === id) || {};
  $('dTitle').textContent = id ? 'Edit Department' : 'New Department';
  $('dId').value = d.id || ''; $('dName').value = d.name || '';
  $('dHead').value = d.headOfficer || ''; $('dEmail').value = d.email || '';
  openModal('departmentModal');
}
async function saveDepartment() {
  const id = $('dId').value;
  const body = {name: $('dName').value.trim(), headOfficer: $('dHead').value.trim(), email: $('dEmail').value.trim() || null};
  try {
    await api(id ? '/departments/' + id : '/departments', id ? 'PUT' : 'POST', body);
    closeModal('departmentModal'); toast('Department saved');
    await loadDepartments(); loadGrievances();
  } catch (e) { toast(e.message, false); }
}
async function deleteDepartment(id) {
  if (!confirm('Delete this department?')) return;
  try { await api('/departments/' + id, 'DELETE'); toast('Department deleted'); loadDepartments(); }
  catch (e) { toast(e.message, false); }
}

// ---------- grievances ----------
async function loadStats() {
  try {
    const s = await api('/stats');
    $('stats').innerHTML = Object.entries(s).map(([k, v]) =>
      `<div class="stat ${k === 'OVERDUE' && v > 0 ? 'bad' : ''}"><b>${v}</b>${k.replace('_', ' ')}</div>`).join('');
  } catch (e) {}
}
async function loadGrievances() {
  const f = $('statusFilter').value;
  try {
    grievances = await api('/grievances' + (f ? '?status=' + f : ''));
    const now = Date.now();
    $('grievanceBody').innerHTML = grievances.map(g => {
      const open = g.status !== 'RESOLVED' && g.status !== 'CLOSED';
      const overdue = open && new Date(g.dueDate).getTime() < now;
      return `<tr>
        <td>${g.id}</td>
        <td><b>${esc(g.title)}</b><br><small>${esc(g.category)}</small></td>
        <td>${esc(g.citizenName)}<br><small>${esc(g.citizenPhone)}</small></td>
        <td>${esc(g.department ? g.department.name : '-')}</td>
        <td><span class="badge ${g.priority}">${g.priority}</span></td>
        <td><span class="badge ${g.status}">${g.status.replace('_', ' ')}</span></td>
        <td>${LEVELS[g.escalationLevel]}</td>
        <td class="${overdue ? 'overdue' : ''}">${fmt(g.dueDate)}${overdue ? '<br>OVERDUE' : ''}</td>
        <td><div class="act">
          <button onclick="openGrievanceForm(${g.id})">Edit</button>
          ${open ? `<button onclick="openAction(${g.id},'status')">Status</button>
          <button onclick="openAction(${g.id},'escalate')" ${g.escalationLevel >= 3 ? 'disabled' : ''}>Escalate</button>
          <button onclick="openAction(${g.id},'resolve')">Resolve</button>` : ''}
          <button onclick="showHistory(${g.id})">History</button>
          <button class="danger" onclick="deleteGrievance(${g.id})">Delete</button>
        </div></td></tr>`;
    }).join('') || '<tr><td colspan="9">No grievances found.</td></tr>';
  } catch (e) { toast(e.message, false); }
  loadStats();
}
function openGrievanceForm(id) {
  const g = grievances.find(x => x.id === id) || {};
  $('gTitle').textContent = id ? 'Edit Grievance #' + id : 'New Grievance';
  $('gId').value = g.id || '';
  $('gTitleInput').value = g.title || ''; $('gDesc').value = g.description || '';
  $('gName').value = g.citizenName || ''; $('gEmail').value = g.citizenEmail || '';
  $('gPhone').value = g.citizenPhone || ''; $('gCategory').value = g.category || '';
  $('gDept').value = g.department ? g.department.id : '';
  $('gPriority').value = g.priority || 'MEDIUM';
  openModal('grievanceModal');
}
async function saveGrievance() {
  const id = $('gId').value;
  const dept = $('gDept').value;
  const body = {
    title: $('gTitleInput').value.trim(), description: $('gDesc').value.trim(),
    citizenName: $('gName').value.trim(), citizenEmail: $('gEmail').value.trim() || null,
    citizenPhone: $('gPhone').value.trim(), category: $('gCategory').value.trim(),
    priority: $('gPriority').value, department: dept ? {id: Number(dept)} : null
  };
  try {
    await api(id ? '/grievances/' + id : '/grievances', id ? 'PUT' : 'POST', body);
    closeModal('grievanceModal'); toast('Grievance saved'); loadGrievances();
  } catch (e) { toast(e.message, false); }
}
async function deleteGrievance(id) {
  if (!confirm('Delete grievance #' + id + '?')) return;
  try { await api('/grievances/' + id, 'DELETE'); toast('Grievance deleted'); loadGrievances(); }
  catch (e) { toast(e.message, false); }
}

// ---------- workflow actions ----------
function openAction(id, type) {
  $('aId').value = id; $('aType').value = type; $('aBy').value = ''; $('aText').value = '';
  $('aStatusWrap').style.display = type === 'status' ? 'block' : 'none';
  $('aTextWrap').style.display = type === 'status' ? 'none' : 'block';
  $('aTitle').textContent = {status: 'Change Status', escalate: 'Escalate Grievance', resolve: 'Resolve Grievance'}[type] + ' #' + id;
  $('aTextLabel').textContent = type === 'resolve' ? 'Resolution note *' : 'Reason for escalation';
  openModal('actionModal');
}
async function submitAction() {
  const id = $('aId').value, type = $('aType').value, by = $('aBy').value.trim();
  try {
    if (type === 'status') await api(`/grievances/${id}/status`, 'POST', {status: $('aStatus').value, actionBy: by});
    if (type === 'escalate') await api(`/grievances/${id}/escalate`, 'POST', {reason: $('aText').value.trim(), actionBy: by});
    if (type === 'resolve') await api(`/grievances/${id}/resolve`, 'POST', {resolution: $('aText').value.trim(), actionBy: by});
    closeModal('actionModal'); toast('Done'); loadGrievances();
  } catch (e) { toast(e.message, false); }
}
async function showHistory(id) {
  try {
    const logs = await api(`/grievances/${id}/history`);
    $('hTitle').textContent = 'History - Grievance #' + id;
    $('historyBody').innerHTML = logs.map(l => `<div class="hist"><b>${esc(l.action)}</b>
      (Level ${l.fromLevel} → ${l.toLevel}) by ${esc(l.actionBy)}<br>${esc(l.reason)}<br><small>${fmt(l.timestamp)}</small></div>`).join('')
      || 'No history.';
    openModal('historyModal');
  } catch (e) { toast(e.message, false); }
}

(async () => { await loadDepartments(); await loadGrievances(); setInterval(loadGrievances, 60000); })();
