# Demo walkthrough

Start the application using the README and open its configured port (8080 by default). Sign in with the administrator credentials from the environment configuration. The H2 demo login is `admin / DemoAdmin123!` unless overridden.

1. Open **Employees**. Check the overview counts, change the page size, and move between pages.
2. Search by name or email. Combine department and status filters, then clear them.
3. Select **Employee ID** in the sort dropdown. Switch between ascending and descending order.
4. Choose **Add employee**, complete the form, and save. Inspect the generated ID and details page.
5. Choose **Edit employee**, change the designation or status, and save.
6. Try a duplicate email to see validation. Salary accepts up to two decimal places.
7. Apply filters and choose **Export CSV**. The export includes all matching records across pages.
8. Open the delete dialog on a disposable record and cancel. Reopen and confirm only when ready to remove it.
9. Open **Departments**. Add a department, assign an employee, and rename it. Check that the employee's department changes. Empty departments can be deleted.
10. Open **API documentation** to inspect Swagger. API writes require a CSRF token; see the authentication guide.
11. Sign out, try an incorrect password, then sign back in.
12. Restart the application and confirm that saved records remain.

The [screenshots](screenshots/README.md) show the employee screens, mobile layout, login, and department management using fictional sample data.
