'use strict';

// The frontend is dependency-free and is served from the same origin as the API.
const icons = {
  users:'<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
  user:'<path d="M20 21v-2a7 7 0 0 0-14 0v2"/><circle cx="13" cy="7" r="4"/>',
  grid:'<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  building:'<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 21v-4h6v4M8 7h1m6 0h1M8 11h1m6 0h1"/>',
  check:'<path d="m5 12 4 4L19 6"/>',
  active:'<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
  inactive:'<circle cx="12" cy="12" r="9"/><path d="M9 9v6m6-6v6"/>',
  plus:'<path d="M12 5v14M5 12h14"/>',
  download:'<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/>',
  search:'<circle cx="10.5" cy="10.5" r="7"/><path d="m16 16 5 5"/>',
  sort:'<path d="m8 8 4-4 4 4M12 4v16m-4-4 4 4 4-4"/>',
  eye:'<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
  edit:'<path d="m16 3 5 5M3 21l5-1L21 7a2.8 2.8 0 0 0-4-4L4 16l-1 5Z"/>',
  trash:'<path d="M3 6h18M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M5 6l1 14h12l1-14M10 10v6m4-6v6"/>',
  left:'<path d="m14 6-6 6 6 6"/>', right:'<path d="m10 6 6 6-6 6"/>',
  arrow:'<path d="M19 12H5m7-7-7 7 7 7"/>',
  lock:'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3m-4 4v3"/>',
  external:'<path d="M15 3h6v6m0-6L10 14M10 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5"/>',
  code:'<path d="m8 7-5 5 5 5m8-10 5 5-5 5m-3-13-2 20"/>',
  spark:'<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z"/>',
  info:'<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10v.1"/>',
  briefcase:'<rect x="3" y="7" width="18" height="14" rx="2"/><path d="M8 7V4h8v3M3 12a24 24 0 0 0 18 0M12 11v4"/>'
};
const icon = name => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[name] || icons.info}</svg>`;
document.querySelectorAll('[data-icon]').forEach(element => { element.innerHTML = icon(element.dataset.icon); });
const main = document.querySelector('#main');
document.querySelector('.skip-link').addEventListener('click', event => {
  event.preventDefault();
  main.focus();
  main.scrollIntoView({block:'start'});
});
const dialog = document.querySelector('#delete-dialog');
const initialList = DirectoryState.defaults;
let state = { ...initialList };
let employees = [];
let departments = [];
let listUrl = '#/employees';
let routeSequence = 0;
let listSequence = 0;
let searchTimer;
let toastTimer;
let deleteTarget = null;
let deleting = false;
let currentUser = null;
let csrfToken = null;
let departmentTarget = null;

const escapeHtml = DirectoryState.escapeHtml;
const initials = name => name.trim().split(/\s+/).filter(Boolean).slice(0,2).map(part => part[0]).join('').toUpperCase();
const colorClass = id => `c${Number(id) % 6}`;
const dateText = value => new Intl.DateTimeFormat('en-IN',{day:'2-digit',month:'short',year:'numeric'}).format(new Date(value + 'T00:00:00'));
const numberText = value => new Intl.NumberFormat('en-IN').format(value);
const salaryText = value => new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:2}).format(value);
const statusPill = status => `<span class="status-pill ${status === 'INACTIVE' ? 'inactive' : ''}"><i></i>${status === 'ACTIVE' ? 'Active' : 'Inactive'}</span>`;

function connection(ok) {
  const element = document.querySelector('#connection');
  element.className = `connection ${ok ? 'online' : 'offline'}`;
  element.innerHTML = `<i></i>${ok ? 'Connected' : 'Connection issue'}`;
}

async function api(path, options = {}) {
  let response;
  const method = (options.method || 'GET').toUpperCase();
  const securityHeaders = {};
  if (!['GET','HEAD','OPTIONS'].includes(method)) {
    if (!csrfToken) {
      const tokenResponse = await fetch('/api/auth/csrf', {credentials:'same-origin'});
      if (!tokenResponse.ok) throw new Error('Could not initialize security. Refresh and try again.');
      csrfToken = await tokenResponse.json();
    }
    securityHeaders[csrfToken.headerName] = csrfToken.token;
  }
  try {
    response = await fetch('/api' + path, { ...options, credentials:'same-origin', headers: { ...securityHeaders, ...(options.body ? {'Content-Type':'application/json'} : {}), ...options.headers } });
  } catch (error) {
    connection(false);
    throw new Error('Unable to reach the server. Check your connection and try again.');
  }
  connection(response.status < 500);
  if (!response.ok) {
    if (response.status === 403) csrfToken = null;
    if (response.status === 401 && currentUser && !path.startsWith('/auth/')) {
      currentUser = null; csrfToken = null; ++routeSequence; ++listSequence;
      document.querySelectorAll('dialog[open]').forEach(item => item.close());
      renderLogin('Your session has expired. Sign in to continue.');
    }
    const problem = await response.json().catch(() => ({}));
    const error = new Error(problem.detail || `Request failed (${response.status}). Please try again.`);
    error.fields = problem.errors || {};
    error.status = response.status;
    throw error;
  }
  if (response.status === 204) return null;
  if (options.csv) return response.blob();
  return response.json();
}

function toast(message, error = false) {
  clearTimeout(toastTimer);
  document.querySelector('#toast-container').innerHTML = `<div class="toast ${error ? 'error' : ''}">${icon(error ? 'info' : 'active')}<span>${escapeHtml(message)}</span></div>`;
  toastTimer = setTimeout(() => { document.querySelector('#toast-container').innerHTML = ''; }, 4500);
}

function queryParams(paginated = true) { return DirectoryState.query(state, paginated); }

function rememberList() {
  listUrl = '#/employees?' + queryParams();
  history.replaceState(null, '', listUrl);
}

async function metadata() {
  const [summary, availableDepartments] = await Promise.all([api('/employees/summary'), api('/employees/departments')]);
  departments = availableDepartments;
  document.querySelector('#nav-count').textContent = summary.total;
  return summary;
}

function stats(summary) {
  const values = [
    ['Total employees',summary.total,'users','','Your entire team, in one place'],
    ['Active employees',summary.active,'active','green','Currently part of your team'],
    ['Inactive employees',summary.inactive,'inactive','orange','Records kept for reference'],
    ['Departments',summary.departments,'building','purple','Teams across your organization']
  ];
  return values.map(([label,value,name,color,hint]) => `<article class="stat-card"><div class="stat-top"><span>${label}</span><span class="stat-icon ${color}">${icon(name)}</span></div><div class="stat-value">${numberText(value)}</div><div class="stat-bottom">${color === 'green' ? '<i></i>' : ''}${hint}</div></article>`).join('');
}

function departmentOptions(selected = '', includeDefaults = false) {
  const values = [...new Set([...(includeDefaults ? ['Engineering','Design','Marketing','People','Finance','Sales','Operations'] : []), ...departments, ...(selected ? [selected] : [])])].sort();
  return values.map(value => `<option value="${escapeHtml(value)}" ${value === selected ? 'selected' : ''}>${escapeHtml(value)}</option>`).join('');
}

async function renderDirectory(sequence) {
  const summary = await metadata();
  if (sequence !== routeSequence) return;
  main.innerHTML = `<div class="page-heading"><div><p class="eyebrow">YOUR PEOPLE, ALL TOGETHER</p><h1>Employees</h1><p class="page-description">A little more organized. A lot more connected.</p></div><div class="heading-actions"><button class="button button-secondary" id="export-button" type="button">${icon('download')}Export CSV</button><a class="button button-primary" href="#/employees/new">${icon('plus')}Add employee</a></div></div>
    <section class="stats-grid" aria-label="Organization overview">${stats(summary)}</section>
    <section class="directory-panel" aria-labelledby="directory-title"><div class="panel-heading"><h2 id="directory-title">Employee directory</h2><span class="count-pill" id="result-count">Loading…</span><span class="panel-description">The people behind the work.</span></div>
    <div class="filters"><div class="search-box">${icon('search')}<label class="sr-only" for="search">Search by name or email</label><input id="search" class="control" type="search" placeholder="Search by name or email…" maxlength="100" value="${escapeHtml(state.search)}" autocomplete="off"></div><label class="sr-only" for="department-filter">Filter by department</label><select id="department-filter" class="control filter-select"><option value="">All departments</option>${departmentOptions(state.department)}</select><label class="sr-only" for="status-filter">Filter by employment status</label><select id="status-filter" class="control filter-select filter-status"><option value="">All statuses</option><option value="ACTIVE" ${state.status === 'ACTIVE' ? 'selected' : ''}>Active</option><option value="INACTIVE" ${state.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option></select><label class="sr-only" for="sort-field">Sort employees by</label><select id="sort-field" class="control sort-select"><option value="fullName">Sort: Name</option><option value="id">Sort: Employee ID</option><option value="department">Sort: Department</option><option value="dateOfJoining">Sort: Date joined</option></select><button id="sort-direction" type="button" class="text-button" aria-label="Reverse sort direction">${state.direction === 'asc' ? 'Ascending' : 'Descending'}</button><span class="filter-spacer"></span><button id="clear-filters" type="button" class="text-button filter-reset" hidden>Clear filters</button></div>
    <div class="table-scroll" id="table-container" aria-live="polite" aria-busy="true"></div><div class="pagination" id="pagination"></div></section>
    <div class="below-panel"><span>${icon('info')}Overview counts include all employees. Filters apply to the directory and export.</span><span>${icon('lock')}Your workspace directory</span></div>`;
  document.querySelector('#search').addEventListener('input', event => {
    clearTimeout(searchTimer);
    state.search = event.target.value;
    state.page = 0;
    searchTimer = setTimeout(() => { rememberList(); loadList(); }, 300);
  });
  for (const [id, key] of [['department-filter','department'],['status-filter','status']]) {
    document.getElementById(id).addEventListener('change', event => { clearTimeout(searchTimer); state[key] = event.target.value; state.page = 0; rememberList(); loadList(); });
  }
  document.querySelector('#clear-filters').addEventListener('click', () => {
    clearTimeout(searchTimer);
    state = { ...state, search:'', department:'', status:'', page:0 };
    document.querySelector('#search').value = '';
    document.querySelector('#department-filter').value = '';
    document.querySelector('#status-filter').value = '';
    rememberList(); loadList();
  });
  const sortField = document.querySelector('#sort-field');
  if (![...sortField.options].some(option => option.value === state.sortBy)) sortField.add(new Option('Sort: ' + state.sortBy, state.sortBy));
  sortField.value = state.sortBy;
  sortField.addEventListener('change', event => { state = DirectoryState.sort({...state,sortBy:''},event.target.value); rememberList(); loadList(); });
  document.querySelector('#sort-direction').addEventListener('click', () => { state = DirectoryState.sort(state,state.sortBy); rememberList(); loadList(); });
  document.querySelector('#export-button').addEventListener('click', exportCsv);
  await loadList();
}

async function loadList() {
  const sequence = ++listSequence;
  const route = routeSequence;
  const container = document.querySelector('#table-container');
  if (!container) return;
  container.setAttribute('aria-busy','true');
  document.querySelector('#sort-field').value = state.sortBy;
  document.querySelector('#sort-direction').textContent = state.direction === 'asc' ? 'Ascending' : 'Descending';
  container.innerHTML = '<div class="loading-row"><span class="spinner"></span>Loading employees…</div>';
  document.querySelector('#clear-filters').hidden = !(state.search || state.department || state.status);
  try {
    const result = await api('/employees?' + queryParams());
    if (sequence !== listSequence || route !== routeSequence) return;
    if (result.totalPages > 0 && state.page >= result.totalPages) {
      state.page = result.totalPages - 1; rememberList(); return loadList();
    }
    employees = result.content;
    document.querySelector('#result-count').textContent = `${numberText(result.totalElements)} ${result.totalElements === 1 ? 'person' : 'people'}`;
    if (!employees.length) {
      const filtered = !!(state.search || state.department || state.status);
      container.innerHTML = `<div class="empty-state"><div class="empty-icon">${icon(filtered ? 'search' : 'users')}</div><h3>${filtered ? 'No matching employees' : 'Your team starts here'}</h3><p>${filtered ? 'Try another name, email, or filter to find who you’re looking for.' : 'Add your first employee to start building your directory.'}</p>${filtered ? '<button class="button button-secondary" id="empty-reset">Clear filters</button>' : `<a class="button button-primary" href="#/employees/new">${icon('plus')}Add employee</a>`}</div>`;
      document.querySelector('#empty-reset')?.addEventListener('click', () => document.querySelector('#clear-filters').click());
    } else {
      container.innerHTML = `<table><caption class="sr-only">Employee directory. Use column headings to sort.</caption><thead><tr>${tableHeading('Employee','fullName') }${tableHeading('Employee ID','id')}${tableHeading('Department','department')}<th scope="col">Designation</th>${tableHeading('Date joined','dateOfJoining')}<th scope="col">Status</th><th scope="col"><span class="sr-only">Actions</span></th></tr></thead><tbody>${employees.map(employee => `<tr><td><div class="employee-cell"><span class="avatar ${colorClass(employee.id)}" aria-hidden="true">${escapeHtml(initials(employee.fullName))}</span><div><a class="employee-name" href="#/employees/${employee.id}">${escapeHtml(employee.fullName)}</a><span class="employee-email">${escapeHtml(employee.email)}</span></div></div></td><td><span class="employee-id">${escapeHtml(employee.employeeId)}</span></td><td><span class="department-chip">${escapeHtml(employee.department)}</span></td><td><span class="designation" title="${escapeHtml(employee.designation)}">${escapeHtml(employee.designation)}</span></td><td>${dateText(employee.dateOfJoining)}</td><td>${statusPill(employee.status)}</td><td><div class="row-actions"><a class="icon-button" href="#/employees/${employee.id}" aria-label="View ${escapeHtml(employee.fullName)}" title="View details">${icon('eye')}</a><a class="icon-button" href="#/employees/${employee.id}/edit" aria-label="Edit ${escapeHtml(employee.fullName)}" title="Edit employee">${icon('edit')}</a><button class="icon-button danger" data-delete="${employee.id}" aria-label="Delete ${escapeHtml(employee.fullName)}" title="Delete employee" type="button">${icon('trash')}</button></div></td></tr>`).join('')}</tbody></table>`;
      container.querySelectorAll('[data-sort]').forEach(button => button.addEventListener('click', () => {
        state = DirectoryState.sort(state, button.dataset.sort); rememberList(); loadList();
      }));
      container.querySelectorAll('[data-delete]').forEach(button => button.addEventListener('click', () => openDelete(employees.find(employee => employee.id === Number(button.dataset.delete)))));
    }
    renderPagination(result);
  } catch (error) {
    if (sequence !== listSequence || route !== routeSequence) return;
    container.innerHTML = errorState(error.message);
    document.querySelector('#result-count').textContent = 'Unable to load';
    document.querySelector('#pagination').innerHTML = '';
    container.querySelector('[data-retry]').addEventListener('click', loadList);
  } finally {
    if (sequence === listSequence && route === routeSequence) container.setAttribute('aria-busy','false');
  }
}

function tableHeading(label, field) {
  const order = state.sortBy === field ? (state.direction === 'asc' ? 'ascending' : 'descending') : 'none';
  return `<th scope="col" aria-sort="${order}"><button type="button" data-sort="${field}" aria-label="Sort by ${label.toLowerCase()}">${label}${icon('sort')}</button></th>`;
}

function renderPagination(result) {
  let pages = [];
  for (let page = 0; page < result.totalPages; page++) {
    if (page === 0 || page === result.totalPages - 1 || Math.abs(page - result.page) <= 1) pages.push(page);
    else if (pages[pages.length-1] !== '…') pages.push('…');
  }
  const from = result.totalElements ? result.page * result.size + 1 : 0;
  const to = Math.min((result.page+1)*result.size,result.totalElements);
  document.querySelector('#pagination').innerHTML = `<div class="pagination-left"><label for="page-size">Rows per page</label><select class="control page-size" id="page-size">${[5,10,20,50,100].map(size => `<option value="${size}" ${state.size === size ? 'selected' : ''}>${size}</option>`).join('')}</select><span class="pagination-range">${numberText(from)}–${numberText(to)} of ${numberText(result.totalElements)} employees</span></div><nav class="pagination-controls" aria-label="Employee list pages"><button class="page-arrow" type="button" data-page="${result.page-1}" aria-label="Previous page" ${result.first ? 'disabled' : ''}>${icon('left')}</button>${pages.map(page => page === '…' ? '<span class="page-ellipsis">…</span>' : `<button class="page-number ${page === result.page ? 'current' : ''}" type="button" data-page="${page}" ${page === result.page ? 'aria-current="page"' : ''} aria-label="Page ${page+1}">${page+1}</button>`).join('')}<button class="page-arrow" type="button" data-page="${result.page+1}" aria-label="Next page" ${result.last ? 'disabled' : ''}>${icon('right')}</button></nav>`;
  document.querySelector('#page-size').addEventListener('change', event => { state.size = Number(event.target.value); state.page = 0; rememberList(); loadList(); });
  document.querySelectorAll('[data-page]').forEach(button => button.addEventListener('click', () => { state.page = Number(button.dataset.page); rememberList(); loadList(); }));
}

async function exportCsv(event) {
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const blob = await api('/employees/export?' + queryParams(false), {csv:true});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a'); link.href = url; link.download = 'employees.csv'; document.body.append(link); link.click(); link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    toast('Employee export downloaded.');
  } catch (error) { toast(error.message,true); }
  finally { button.disabled = false; }
}

function backLink() { return `<a class="back-link" href="${escapeHtml(listUrl)}">${icon('arrow')}Back to employees</a>`; }

async function renderDetails(id, sequence) {
  const [employee] = await Promise.all([api('/employees/' + id), metadata()]);
  if (sequence !== routeSequence) return;
  main.innerHTML = `${backLink()}<div class="page-heading"><div><p class="eyebrow">EMPLOYEE RECORD</p><h1>Employee details</h1><p class="page-description">A closer look at the person behind the role.</p></div><div class="heading-actions"><button class="button button-secondary danger-link" id="detail-delete" type="button">${icon('trash')}Delete</button><a class="button button-primary" href="#/employees/${id}/edit">${icon('edit')}Edit employee</a></div></div><section class="profile-header" aria-label="Employee profile"><span class="avatar ${colorClass(id)}" aria-hidden="true">${escapeHtml(initials(employee.fullName))}</span><div class="profile-title"><h2>${escapeHtml(employee.fullName)}</h2><p>${escapeHtml(employee.designation)} · ${escapeHtml(employee.department)}</p>${statusPill(employee.status)}</div><span class="profile-id">${escapeHtml(employee.employeeId)}</span></section><div class="details-grid"><section class="detail-card"><div class="section-title"><span>${icon('user')}</span><h2>Personal information</h2></div><dl class="detail-list">${detailItem('Full name',employee.fullName)}${detailItem('Email address',employee.email,`mailto:${employee.email}`)}${detailItem('Phone number',employee.phoneNumber,`tel:${employee.phoneNumber.replace(/[^+\d]/g,'')}`)}${detailItem('Employee ID',employee.employeeId)}</dl></section><section class="detail-card"><div class="section-title"><span>${icon('briefcase')}</span><h2>Employment details</h2></div><dl class="detail-list">${detailItem('Department',employee.department)}${detailItem('Designation',employee.designation)}${detailItem('Date of joining',dateText(employee.dateOfJoining))}${detailItem('Annual salary (INR)',salaryText(employee.salary))}</dl></section></div><p class="record-meta">Created ${new Date(employee.createdAt).toLocaleString('en-IN')} · Last updated ${new Date(employee.updatedAt).toLocaleString('en-IN')}</p>`;
  document.querySelector('#detail-delete').addEventListener('click', () => openDelete(employee));
}

function detailItem(label, value, href) {
  return `<div class="detail-item"><dt>${label}</dt><dd>${href ? `<a href="${escapeHtml(href)}">${escapeHtml(value)}</a>` : escapeHtml(value)}</dd></div>`;
}

async function renderForm(id, sequence) {
  const [employee] = await Promise.all([id ? api('/employees/' + id) : Promise.resolve(null), metadata()]);
  if (sequence !== routeSequence) return;
  const value = name => escapeHtml(employee?.[name] ?? '');
  const cancelUrl = employee ? `#/employees/${id}` : listUrl;
  main.innerHTML = `${backLink()}<div class="page-heading"><div><p class="eyebrow">GROW YOUR DIRECTORY</p><h1>${employee ? 'Edit employee' : 'Add employee'}</h1><p class="page-description">${employee ? 'Keep their information accurate and up to date.' : 'A new person. A new part of your team.'}</p></div></div><div class="editor-layout"><form class="form-card" id="employee-form"><section class="form-section"><div class="section-title"><span>${icon('user')}</span><div><h2>Personal information</h2><p>The essentials for getting to know your employee.</p></div></div><div class="form-grid">${field('fullName','Full name',`<input class="control" id="fullName" name="fullName" value="${value('fullName')}" placeholder="e.g. Ananya Sharma" maxlength="100" autocomplete="name" required>`)}${field('employeeId','Employee ID',`<input class="control" id="employeeId" value="${employee ? value('employeeId') : 'Assigned automatically'}" readonly>`,'A unique ID is generated when the record is created.',false)}${field('email','Email address',`<input class="control" type="email" id="email" name="email" value="${value('email')}" placeholder="name@example.com" maxlength="254" autocomplete="email" required>`)}${field('phoneNumber','Phone number',`<input class="control" type="tel" id="phoneNumber" name="phoneNumber" value="${value('phoneNumber')}" placeholder="+91 98765 43210" minlength="7" maxlength="20" autocomplete="tel" required>`,'Include the country code if applicable.')}</div></section><section class="form-section"><div class="section-title"><span>${icon('briefcase')}</span><div><h2>Employment details</h2><p>Where they work and how they contribute.</p></div></div><div class="form-grid">${field('department','Department',`<input class="control" id="department" name="department" value="${value('department')}" placeholder="Select or enter a department" maxlength="80" list="department-options" required><datalist id="department-options">${departmentOptions(employee?.department || '',true)}</datalist>`)}${field('designation','Designation',`<input class="control" id="designation" name="designation" value="${value('designation')}" placeholder="e.g. Backend Engineer" maxlength="100" required>`)}${field('dateOfJoining','Date of joining',`<input class="control" type="date" id="dateOfJoining" name="dateOfJoining" value="${value('dateOfJoining')}" required>`)}${field('salary','Annual salary (INR)',`<input class="control" type="number" id="salary" name="salary" value="${value('salary')}" placeholder="e.g. 1200000" min="0" max="999999999999.99" step="0.01" required>`)}${field('status','Employment status',`<select class="control" id="status" name="status" required><option value="ACTIVE" ${employee?.status !== 'INACTIVE' ? 'selected' : ''}>Active</option><option value="INACTIVE" ${employee?.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option></select>`)}</div></section><div id="form-error" class="form-alert" role="alert" tabindex="-1" hidden></div><div class="form-actions"><a class="button button-secondary" href="${escapeHtml(cancelUrl)}">Cancel</a><button type="submit" id="save-employee" class="button button-primary">${icon(employee ? 'check' : 'plus')}${employee ? 'Save changes' : 'Add employee'}</button></div></form><aside class="form-aside"><span>${icon('info')}</span><h3>A complete record matters.</h3><p>Keep contact information and employment details accurate so your directory stays useful for everyone.</p><div class="aside-rule"></div><p>Fields marked with an asterisk (*) are required. Each employee needs a unique email address.</p><div class="aside-rule"></div><p>Salary is recorded as an annual amount in Indian rupees (INR).</p></aside></div>`;
  const form = document.querySelector('#employee-form');
  form.addEventListener('input', event => {
    if (event.target.id) { event.target.removeAttribute('aria-invalid'); const error = document.getElementById(event.target.id + '-error'); if (error) error.textContent = ''; }
  });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const button = document.querySelector('#save-employee');
    const alert = document.querySelector('#form-error');
    document.querySelectorAll('.field-error').forEach(element => { element.textContent = ''; });
    form.querySelectorAll('[aria-invalid]').forEach(element => element.removeAttribute('aria-invalid'));
    alert.hidden = true;
    const data = Object.fromEntries(new FormData(form));
    for (const key of ['fullName','email','phoneNumber','department','designation']) data[key] = data[key].trim();
    // Preserve the decimal text; the backend binds it directly to BigDecimal.
    button.disabled = true;
    button.textContent = 'Saving…';
    try {
      const saved = await api('/employees' + (id ? '/' + id : ''), {method:id ? 'PUT' : 'POST',body:JSON.stringify(data)});
      toast(employee ? 'Employee information updated.' : 'Employee added to your team.');
      location.hash = '#/employees/' + saved.id;
    } catch (error) {
      alert.textContent = error.message; alert.hidden = false;
      for (const [name,message] of Object.entries(error.fields || {})) {
        document.getElementById(name)?.setAttribute('aria-invalid','true');
        const target = document.getElementById(name+'-error'); if (target) target.textContent = message;
      }
      const first = form.querySelector('[aria-invalid=true]'); (first || alert).focus();
    } finally {
      button.disabled = false; button.innerHTML = icon(employee ? 'check' : 'plus') + (employee ? 'Save changes' : 'Add employee');
    }
  });
}

function field(name, label, control, hint = '', required = true) {
  const describedBy = `${hint ? `${name}-hint ` : ''}${name}-error`;
  return `<div class="field"><label for="${name}">${label}${required ? '<em aria-hidden="true">*</em>' : ''}</label>${control.replace('class="control"',`class="control" aria-describedby="${describedBy.trim()}"`)}${hint ? `<p class="field-hint" id="${name}-hint">${hint}</p>` : ''}<p class="field-error" id="${name}-error"></p></div>`;
}

function openDelete(employee) {
  if (!employee) return;
  deleteTarget = employee;
  document.querySelector('#delete-description').textContent = `Are you sure you want to delete ${employee.fullName} (${employee.employeeId})?`;
  document.querySelector('#delete-error').hidden = true;
  dialog.showModal();
}
document.querySelector('#cancel-delete').addEventListener('click', () => { if (!deleting) dialog.close(); });
dialog.addEventListener('cancel', event => { if (deleting) event.preventDefault(); });
document.querySelector('#confirm-delete').addEventListener('click', async () => {
  if (!deleteTarget || deleting) return;
  deleting = true;
  const button = document.querySelector('#confirm-delete');
  button.disabled = true; button.textContent = 'Deleting…'; document.querySelector('#cancel-delete').disabled = true;
  try {
    await api('/employees/' + deleteTarget.id, {method:'DELETE'});
    dialog.close(); toast('Employee record deleted.');
    if (/^#\/employees\/?(?:\?|$)/.test(location.hash)) { const summary = await metadata(); document.querySelector('.stats-grid').innerHTML = stats(summary); await loadList(); }
    else location.hash = listUrl;
  } catch (error) { const alert = document.querySelector('#delete-error'); alert.textContent = error.message; alert.hidden = false; }
  finally { deleting = false; button.disabled = false; button.textContent = 'Delete employee'; document.querySelector('#cancel-delete').disabled = false; }
});

function errorState(message) { return `<div class="empty-state"><div class="empty-icon">${icon('info')}</div><h3>We couldn’t load this page</h3><p>${escapeHtml(message)}</p><button type="button" class="button button-secondary" data-retry>Try again</button></div>`; }

function renderLogin(message = '') {
  document.body.classList.add('signed-out');
  document.title = 'Sign in — People';
  main.innerHTML = `<div class="login-layout"><section class="login-story"><a class="brand" href="#/employees">people<span class="brand-dot">.</span></a><p class="eyebrow">YOUR PEOPLE, ALL TOGETHER</p><h1>Good teams start<br>with good people.</h1><p>Your team directory, thoughtfully organized.<br>Sign in to manage the people behind the work.</p><div class="login-art" aria-hidden="true"><span>${icon('users')}</span><span>${icon('building')}</span><span>${icon('briefcase')}</span></div><small>Employee Management Workspace</small></section><section class="login-card"><span class="login-lock">${icon('lock')}</span><h2>Welcome back</h2><p>Sign in to your workspace to continue.</p><form id="login-form"><div class="field"><label for="username">Username</label><input class="control" id="username" name="username" autocomplete="username" maxlength="100" required autofocus></div><div class="field"><label for="password">Password</label><input class="control" id="password" name="password" type="password" autocomplete="current-password" required></div><p class="form-alert" id="login-error" role="alert" ${message ? '' : 'hidden'}>${escapeHtml(message)}</p><button class="button button-primary" type="submit" id="login-submit">Sign in ${icon('right')}</button></form><p class="login-help">Use the administrator account configured for this workspace.</p></section></div>`;
  document.querySelector('#login-form').addEventListener('submit', async event => {
    event.preventDefault();
    const button = document.querySelector('#login-submit');
    const errorBox = document.querySelector('#login-error');
    button.disabled = true; button.textContent = 'Signing in…'; errorBox.hidden = true;
    try {
      await api('/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(new FormData(event.target)).toString()});
      csrfToken = null;
      currentUser = await api('/auth/me');
      event.target.reset();
      if (location.hash === '#/login') location.hash = '#/employees'; else await route();
    } catch (error) {
      errorBox.textContent = error.message; errorBox.hidden = false;
      document.querySelector('#password').value = ''; document.querySelector('#password').focus();
    } finally { button.disabled = false; button.innerHTML = 'Sign in ' + icon('right'); }
  });
}

document.querySelector('#logout-button').addEventListener('click',async event => {
  const button = event.currentTarget; button.disabled = true;
  try {
    await api('/auth/logout',{method:'POST'});
    currentUser = null; csrfToken = null; ++routeSequence; ++listSequence;
    employees = []; departments = []; state = {...initialList}; listUrl = '#/employees';
    document.querySelector('#nav-count').textContent = '—';
    location.hash = '#/login'; renderLogin();
  } catch (error) { toast(error.message,true); }
  finally { button.disabled = false; }
});

async function renderDepartments(sequence) {
  const [items] = await Promise.all([api('/departments'),metadata()]);
  if (sequence !== routeSequence) return;
  main.innerHTML = `<div class="page-heading"><div><p class="eyebrow">ROOM FOR EVERY TEAM</p><h1>Departments</h1><p class="page-description">Organize your teams and keep everyone connected.</p></div><button class="button button-primary" id="add-department">${icon('plus')}Add department</button></div><section class="department-grid" aria-label="Departments">${items.map(item => `<article class="department-card"><span class="stat-icon green">${icon('building')}</span><h2>${escapeHtml(item.name)}</h2><p>${numberText(item.employeeCount)} ${item.employeeCount === 1 ? 'employee' : 'employees'}</p><div class="department-actions"><a class="text-button" href="#/employees?department=${encodeURIComponent(item.name)}">View employees ${icon('right')}</a><button class="icon-button" data-rename-department="${item.id}" aria-label="Rename ${escapeHtml(item.name)}">${icon('edit')}</button><button class="icon-button danger" data-delete-department="${item.id}" aria-label="Delete ${escapeHtml(item.name)}" ${item.employeeCount ? 'disabled title="Move employees before deleting this department"' : ''}>${icon('trash')}</button></div></article>`).join('') || '<div class="empty-state"><h2>Make room for your first team</h2><p>Add a department or assign one when creating an employee.</p></div>'}</section><p class="department-note">Renaming a department updates its employees. Move employees before deleting a department.</p>`;
  document.querySelector('#add-department').addEventListener('click',() => openDepartment(null));
  main.querySelectorAll('[data-rename-department]').forEach(button => button.addEventListener('click',() => openDepartment(items.find(item => item.id === Number(button.dataset.renameDepartment)))));
  main.querySelectorAll('[data-delete-department]').forEach(button => button.addEventListener('click',() => openDepartment(items.find(item => item.id === Number(button.dataset.deleteDepartment)),true)));
}

function openDepartment(item, removing = false) {
  departmentTarget = item ? {...item,removing} : null;
  const target = document.querySelector('#department-dialog');
  document.querySelector('#department-dialog-title').textContent = removing ? 'Delete department?' : item ? 'Rename department' : 'Add department';
  document.querySelector('#department-dialog-description').textContent = removing ? `Delete ${item.name}? Only empty departments can be deleted.` : item ? 'The new name will apply to all employees in this department.' : 'Create a team for your employees.';
  document.querySelector('.department-name-field').hidden = removing;
  const input = document.querySelector('#department-name'); input.value = item?.name || ''; input.required = !removing;
  document.querySelector('#department-error').hidden = true;
  const button = document.querySelector('#department-save'); button.textContent = removing ? 'Delete department' : 'Save department'; button.classList.toggle('button-danger',removing); button.classList.toggle('button-primary',!removing);
  target.showModal();
}
document.querySelector('#department-cancel').addEventListener('click',() => document.querySelector('#department-dialog').close());
document.querySelector('#department-form').addEventListener('submit',async event => {
  event.preventDefault();
  const button = document.querySelector('#department-save'); button.disabled = true;
  const cancel = document.querySelector('#department-cancel'); cancel.disabled = true;
  const target = departmentTarget;
  try {
    await api('/departments' + (target ? '/' + target.id : ''),{method:target?.removing ? 'DELETE' : target ? 'PUT' : 'POST',...(target?.removing ? {} : {body:JSON.stringify({name:document.querySelector('#department-name').value.trim()})})});
    document.querySelector('#department-dialog').close(); toast(target?.removing ? 'Department deleted.' : target ? 'Department updated.' : 'Department created.'); await route();
  } catch (error) { const box = document.querySelector('#department-error'); box.textContent = error.message; box.hidden = false; }
  finally { button.disabled = false; cancel.disabled = false; }
});
document.querySelector('#department-dialog').addEventListener('cancel',event => { if (document.querySelector('#department-save').disabled) event.preventDefault(); });

async function route() {
  clearTimeout(searchTimer);
  ++listSequence;
  const sequence = ++routeSequence;
  const hash = location.hash.slice(1) || '/employees';
  const [path, query] = hash.split('?');
  main.innerHTML = '<div class="initial-loading"><span class="spinner"></span>Loading your workspace…</div>';
  document.querySelector('#breadcrumb-current').textContent = 'Employees';
  try {
    if (!currentUser) {
      try { currentUser = await api('/auth/me'); }
      catch (error) { if (error.status === 401) { renderLogin(); return; } throw error; }
    }
    document.body.classList.remove('signed-out');
    document.querySelector('.admin strong').textContent = currentUser.username;
    document.querySelectorAll('.nav-item').forEach(link => link.classList.toggle('selected',link.getAttribute('href') === (path === '/departments' ? '#/departments' : '#/employees')));
    if (path === '/departments') {
      document.title = 'Departments — People'; document.querySelector('#breadcrumb-current').textContent = 'Departments'; await renderDepartments(sequence);
    } else if (path === '/employees' || path === '/' || path === '/login') {
      state = DirectoryState.parse(query);
      listUrl = '#/employees' + (query ? '?' + query : '');
      document.title = 'Employees — People';
      await renderDirectory(sequence);
    } else if (path === '/employees/new') {
      document.title = 'Add employee — People'; document.querySelector('#breadcrumb-current').textContent = 'Add employee'; await renderForm(null,sequence);
    } else {
      const match = path.match(/^\/employees\/([1-9]\d*)(\/edit)?$/);
      if (!match) { main.innerHTML = `${backLink()}<div class="empty-state"><h1>Page not found</h1><p>Return to the employee directory to continue.</p></div>`; return; }
      document.title = `${match[2] ? 'Edit employee' : 'Employee details'} — People`;
      document.querySelector('#breadcrumb-current').textContent = match[2] ? 'Edit employee' : 'Employee details';
      if (match[2]) await renderForm(match[1],sequence); else await renderDetails(match[1],sequence);
    }
  } catch (error) {
    if (sequence !== routeSequence) return;
    main.innerHTML = backLink() + errorState(error.message);
    main.querySelector('[data-retry]').addEventListener('click',route);
  }
}
window.addEventListener('hashchange', () => { route(); window.scrollTo({top:0}); });
route();
