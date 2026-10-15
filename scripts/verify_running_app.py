"""Exercise a running localhost app using disposable records, then clean up.

Uses only Python's standard library. Reads generated local credentials from .env,
or APP_ADMIN_USERNAME/APP_ADMIN_PASSWORD and BASE_URL from the environment.
"""
from pathlib import Path
from urllib.request import Request, build_opener, HTTPCookieProcessor
from urllib.error import HTTPError
from urllib.parse import urlencode, urlparse
from http.cookiejar import CookieJar
import base64
import json
import os
import uuid
import argparse
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--restart-containers', action='store_true', help='Restart only the people-enhancements app/db containers and verify record persistence.')
args = parser.parse_args()

root = Path(__file__).resolve().parent.parent
config = {}
if (root / '.env').exists():
    for line in (root / '.env').read_text().splitlines():
        if line and not line.startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            config[key] = value
config.update(os.environ)
base = config.get('BASE_URL', 'http://127.0.0.1:' + config.get('PORT', '8080')).rstrip('/')
if urlparse(base).hostname not in {'localhost', '127.0.0.1', '::1'}:
    raise SystemExit('This smoke test is restricted to localhost.')
username, password = config.get('APP_ADMIN_USERNAME', 'admin'), config.get('APP_ADMIN_PASSWORD', '')
if not password:
    raise SystemExit('Set APP_ADMIN_PASSWORD or configure the local .env.')
basic = 'Basic ' + base64.b64encode((username + ':' + password).encode()).decode()

class Client:
    def __init__(self, auth=None):
        self.auth = auth
        self.opener = build_opener(HTTPCookieProcessor(CookieJar()))

    def call(self, method, path, body=None, expected=200, protected=True, form=False):
        headers = {'Accept': 'text/csv' if path.startswith('/api/employees/export') else 'application/json'}
        if self.auth:
            headers['Authorization'] = self.auth
        if method not in {'GET','HEAD','OPTIONS'} and protected:
            token = self.call('GET','/api/auth/csrf')
            headers[token['headerName']] = token['token']
        data = None
        if body is not None:
            data = (urlencode(body) if form else json.dumps(body)).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
        try:
            response = self.opener.open(Request(base + path, data=data, headers=headers, method=method), timeout=30)
        except HTTPError as error:
            response = error
        content = response.read().decode()
        assert response.status == expected, (method, path, response.status, content)
        if response.headers.get('Content-Type','').startswith(('application/json','application/problem+json')) and content:
            return json.loads(content)
        return content

anonymous, client = Client(), Client(basic)
anonymous.call('GET','/api/employees',expected=401)
anonymous.call('POST','/api/auth/login',{'username':username,'password':password},expected=403,protected=False,form=True)
anonymous.call('POST','/api/auth/login',{'username':username,'password':'incorrect'},expected=401,form=True)
anonymous.call('POST','/api/auth/login',{'username':username,'password':password},expected=204,form=True)
assert anonymous.call('GET','/api/auth/me')['username'] == username
anonymous.call('POST','/api/auth/logout',expected=204)
anonymous.call('GET','/api/auth/me',expected=401)
client.call('DELETE','/api/employees/999999',expected=403,protected=False)
assert client.call('GET','/api/auth/me')['role'] == 'ADMIN'
print('PASS: session login/logout, wrong-password rejection, HTTP Basic, unauthenticated denial, and CSRF enforcement')

tag = 'Smoke ' + uuid.uuid4().hex[:10]
department_id = None
employee_ids = []
try:
    department = client.call('POST','/api/departments',{'name':tag},expected=201)
    department_id = department['id']
    client.call('POST','/api/departments',{'name':tag.lower()},expected=409)
    for i, name in enumerate(['Zara Smoke','Aarav Smoke']):
        employee = client.call('POST','/api/employees',{'fullName':name,'email':uuid.uuid4().hex+'@example.com',
            'phoneNumber':'+91 98765 43210','department':tag,'designation':'QA Engineer','dateOfJoining':'2025-01-10',
            'salary':'1234567.25','status':'ACTIVE'},expected=201)
        employee_ids.append(employee['id'])
    for direction, expected_ids in [('asc',employee_ids),('desc',list(reversed(employee_ids)))]:
        page = client.call('GET','/api/employees?' + urlencode({'department':tag,'sortBy':'id','direction':direction}))
        assert [e['id'] for e in page['content']] == expected_ids
    if args.restart_containers:
        subprocess.run(['docker','compose','-p','people-enhancements','restart','db','app'], cwd=root, check=True)
        deadline = time.monotonic() + 60
        while True:
            try:
                persisted = client.call('GET',f'/api/employees/{employee_ids[0]}')
                assert persisted['fullName'] == 'Zara Smoke'
                break
            except (OSError, AssertionError):
                if time.monotonic() >= deadline:
                    raise
                time.sleep(2)
        print('PASS: employee records persist after restarting the isolated application and PostgreSQL containers')
    client.call('DELETE',f'/api/departments/{department_id}',expected=409)
    renamed = tag + ' Updated'
    client.call('PUT',f'/api/departments/{department_id}',{'name':renamed})
    detail = client.call('GET',f'/api/employees/{employee_ids[0]}')
    assert detail['department'] == renamed and detail['salary'] == 1234567.25
    update = {k:detail[k] for k in ['fullName','email','phoneNumber','department','designation','dateOfJoining','salary','status']}
    update['status'] = 'INACTIVE'
    client.call('PUT',f'/api/employees/{employee_ids[0]}',update)
    filtered = client.call('GET','/api/employees?' + urlencode({'department':renamed,'status':'INACTIVE','search':'Zara'}))
    assert filtered['totalElements'] == 1
    csv = client.call('GET','/api/employees/export?' + urlencode({'department':renamed,'sortBy':'id'}))
    assert 'Zara Smoke' in csv and 'Aarav Smoke' in csv
    print('PASS: PostgreSQL employee CRUD, numeric ID sorting both ways, filters, salary precision, CSV, department rename propagation, and in-use deletion protection')
finally:
    for employee_id in employee_ids:
        client.call('DELETE',f'/api/employees/{employee_id}',expected=204)
    if department_id is not None:
        client.call('DELETE',f'/api/departments/{department_id}',expected=204)
print('PASS: disposable test records cleaned up')
