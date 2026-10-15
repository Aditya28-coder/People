const {test} = require('node:test');
const assert = require('node:assert/strict');
const directory = require('../../src/main/resources/static/directory-state.js');

test('employee ID sorting toggles numeric backend sort and resets pagination', () => {
  const first = directory.sort({...directory.defaults,page:4},'id');
  assert.equal(first.sortBy,'id'); assert.equal(first.direction,'asc'); assert.equal(first.page,0);
  assert.equal(directory.sort(first,'id').direction,'desc');
});
test('switching columns starts ascending and preserves filters', () => {
  const result = directory.sort({...directory.defaults,search:'Ada',department:'Design',direction:'desc'},'department');
  assert.equal(result.direction,'asc'); assert.equal(result.search,'Ada'); assert.equal(result.department,'Design');
});
test('bookmark round trip retains ID direction, filters, page and page size', () => {
  const state = {...directory.defaults,search:'Name & email',department:'R&D',status:'INACTIVE',sortBy:'id',direction:'desc',page:3,size:20};
  assert.deepEqual(directory.parse(directory.query(state).toString()),state);
});
test('malformed bookmark values fall back to supported defaults', () => {
  assert.deepEqual(directory.parse('sortBy=password&direction=sideways&page=-1&size=999&status=bad'),directory.defaults);
  assert.equal(directory.parse('page=NaN').page,0); assert.equal(directory.parse('page=1.2').page,0);
});
test('CSV query retains filters and sort but excludes pagination', () => {
  const query = directory.query({...directory.defaults,page:7,size:5,department:'Finance',sortBy:'id'},false);
  assert.equal(query.has('page'),false); assert.equal(query.has('size'),false);
  assert.equal(query.get('department'),'Finance'); assert.equal(query.get('sortBy'),'id');
});
test('HTML-sensitive employee text cannot create markup or attributes', () => {
  assert.equal(directory.escapeHtml('<img src=x onerror="bad()"> & \'test\''),'&lt;img src=x onerror=&quot;bad()&quot;&gt; &amp; &#39;test&#39;');
  assert.equal(directory.escapeHtml(null),'');
});
test('untrusted unsupported sort fields are rejected', () => {
  assert.throws(() => directory.sort(directory.defaults,'password'),/Unsupported/);
});
test('bookmarked filter text is capped to backend field limits', () => {
  const result = directory.parse(new URLSearchParams({search:'a'.repeat(120),department:'b'.repeat(100)}));
  assert.equal(result.search.length,100); assert.equal(result.department.length,80);
});
