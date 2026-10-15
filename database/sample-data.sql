-- Fictional sample records for local evaluation; all emails use example.com.
-- Safe to rerun: existing employee emails and department names are retained.
-- Run against the application's PostgreSQL database with psql -v ON_ERROR_STOP=1.
BEGIN;

WITH added AS (
    INSERT INTO departments (name, name_key)
    VALUES
        ('Design', 'design'),
        ('Engineering', 'engineering'),
        ('Finance', 'finance'),
        ('Marketing', 'marketing'),
        ('People', 'people'),
        ('Sales', 'sales')
    ON CONFLICT DO NOTHING
    RETURNING id
) SELECT count(*) AS departments_added FROM added;

WITH sample(full_name, email, phone_number, department, designation, date_of_joining, salary, status) AS (
    VALUES
        ('Aarav Mehta', 'aarav.mehta@example.com', '+91 98765 43000', 'Engineering', 'Senior Backend Engineer', '2023-06-12', 1800000, 'ACTIVE'),
        ('Aditi Rao', 'aditi.rao@example.com', '+91 98765 43001', 'Design', 'Product Designer', '2024-02-19', 1200000, 'ACTIVE'),
        ('Arjun Kapoor', 'arjun.kapoor@example.com', '+91 98765 43002', 'Engineering', 'Frontend Engineer', '2024-07-08', 1400000, 'ACTIVE'),
        ('Diya Nair', 'diya.nair@example.com', '+91 98765 43003', 'People', 'People Operations Lead', '2022-11-14', 1500000, 'ACTIVE'),
        ('Ishaan Shah', 'ishaan.shah@example.com', '+91 98765 43004', 'Marketing', 'Growth Manager', '2023-09-04', 1350000, 'ACTIVE'),
        ('Kavya Iyer', 'kavya.iyer@example.com', '+91 98765 43005', 'Engineering', 'QA Engineer', '2024-04-22', 1050000, 'ACTIVE'),
        ('Meera Joshi', 'meera.joshi@example.com', '+91 98765 43006', 'Finance', 'Financial Analyst', '2023-01-16', 1150000, 'INACTIVE'),
        ('Neel Verma', 'neel.verma@example.com', '+91 98765 43007', 'Engineering', 'Backend Engineer', '2025-01-13', 1300000, 'ACTIVE'),
        ('Priya Menon', 'priya.menon@example.com', '+91 98765 43008', 'Design', 'Design Lead', '2022-08-01', 1900000, 'ACTIVE'),
        ('Rohan Desai', 'rohan.desai@example.com', '+91 98765 43009', 'Sales', 'Account Executive', '2024-03-11', 1100000, 'ACTIVE'),
        ('Saanvi Gupta', 'saanvi.gupta@example.com', '+91 98765 43010', 'Marketing', 'Content Strategist', '2024-09-02', 950000, 'ACTIVE'),
        ('Vihaan Reddy', 'vihaan.reddy@example.com', '+91 98765 43011', 'Engineering', 'DevOps Engineer', '2023-05-15', 1600000, 'ACTIVE'),
        ('Ananya Sharma', 'ananya.sharma@example.com', '+91 98765 43012', 'People', 'Talent Acquisition Specialist', '2025-02-03', 1000000, 'ACTIVE'),
        ('Dev Patel', 'dev.patel@example.com', '+91 98765 43013', 'Finance', 'Finance Manager', '2022-04-18', 1750000, 'ACTIVE'),
        ('Ira Mukherjee', 'ira.mukherjee@example.com', '+91 98765 43014', 'Sales', 'Sales Development Representative', '2024-12-09', 850000, 'INACTIVE'),
        ('Kabir Sinha', 'kabir.sinha@example.com', '+91 98765 43015', 'Engineering', 'Platform Engineer', '2023-10-23', 1550000, 'ACTIVE'),
        ('Nisha Kumar', 'nisha.kumar@example.com', '+91 98765 43016', 'Design', 'UX Researcher', '2024-06-17', 1250000, 'ACTIVE'),
        ('Om Prakash', 'om.prakash@example.com', '+91 98765 43017', 'Marketing', 'SEO Specialist', '2025-04-07', 900000, 'ACTIVE'),
        ('Riya Bose', 'riya.bose@example.com', '+91 98765 43018', 'Engineering', 'Engineering Manager', '2022-03-14', 2400000, 'ACTIVE'),
        ('Sahil Jain', 'sahil.jain@example.com', '+91 98765 43019', 'Sales', 'Sales Manager', '2023-08-21', 1700000, 'INACTIVE'),
        ('Tara Bhat', 'tara.bhat@example.com', '+91 98765 43020', 'People', 'HR Business Partner', '2024-01-08', 1400000, 'ACTIVE'),
        ('Ved Malhotra', 'ved.malhotra@example.com', '+91 98765 43021', 'Finance', 'Accounts Associate', '2025-06-02', 750000, 'ACTIVE'),
        ('Zara Khan', 'zara.khan@example.com', '+91 98765 43022', 'Engineering', 'Full Stack Engineer', '2024-10-14', 1450000, 'ACTIVE'),
        ('Yash Agarwal', 'yash.agarwal@example.com', '+91 98765 43023', 'Engineering', 'Graduate Engineer', '2025-07-14', 800000, 'ACTIVE')
), added AS (
    INSERT INTO employees (full_name, email, phone_number, department, designation, date_of_joining, salary, status, created_at, updated_at, version)
    SELECT s.full_name, s.email, s.phone_number, d.name, s.designation,
           s.date_of_joining::date, s.salary, s.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
    FROM sample s
    JOIN departments d ON d.name_key = LOWER(s.department)
    WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE LOWER(e.email) = s.email)
    ON CONFLICT (email) DO NOTHING
    RETURNING id
) SELECT count(*) AS employees_added FROM added;

COMMIT;
