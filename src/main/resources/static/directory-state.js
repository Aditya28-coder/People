/* Shared state rules are also exercised by Node's built-in test runner. */
(function (root) {
  'use strict';
  const defaults = {search:'',department:'',status:'',page:0,size:10,sortBy:'fullName',direction:'asc'};
  const sortFields = ['id','fullName','email','department','designation','dateOfJoining','salary','status'];
  function parse(query) {
    const params = new URLSearchParams(query || '');
    const page = Number(params.get('page') || 0);
    const size = Number(params.get('size') || 10);
    return {search:(params.get('search') || '').slice(0,100),department:(params.get('department') || '').slice(0,80),
      status:['ACTIVE','INACTIVE'].includes(params.get('status')) ? params.get('status') : '',
      page:Number.isInteger(page) && page >= 0 ? page : 0,size:[5,10,20,50,100].includes(size) ? size : 10,
      sortBy:sortFields.includes(params.get('sortBy')) ? params.get('sortBy') : 'fullName',
      direction:params.get('direction') === 'desc' ? 'desc' : 'asc'};
  }
  function query(state, paginated = true) {
    const params = new URLSearchParams();
    for (const key of ['search','department','status','sortBy','direction']) if (state[key]) params.set(key,state[key]);
    if (paginated) { params.set('page',state.page); params.set('size',state.size); }
    return params;
  }
  function sort(state, field) {
    if (!sortFields.includes(field)) throw new Error('Unsupported sort field');
    return {...state,sortBy:field,direction:state.sortBy === field && state.direction === 'asc' ? 'desc' : 'asc',page:0};
  }
  function escapeHtml(value) { return String(value ?? '').replace(/[&<>"']/g,char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char])); }
  const api = {defaults,parse,query,sort,escapeHtml};
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.DirectoryState = api;
})(globalThis);
