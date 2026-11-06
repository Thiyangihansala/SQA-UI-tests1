# LinkedIn UI tests (SQA Assignment 04)

Java 17+ / Selenium WebDriver / TestNG example implementing login, negative login,
search, and opening the first search result using Page Object Model classes.

## Important safety and account notice

LinkedIn restricts automated access and may present security checks or limit accounts.
Review LinkedIn's current User Agreement and your course instructions before running
live tests. Use only an account you own and have permission to test. No test in this
project attempts to solve or bypass CAPTCHA or verification. Stop if a challenge appears.
Repeated or unattended runs are not recommended.

Never send account credentials in chat, add them to source code, or commit them.
The tests read credentials from environment variables, and live browser activity is
disabled unless you explicitly set `LINKEDIN_LIVE_TESTS=true`.

## Requirements

- Java 17 or later
- Maven 3.8+
- Google Chrome
- Internet access for Maven dependencies and the live tests

Selenium Manager obtains a compatible ChromeDriver when needed; no driver binary is
checked into this project.

## Configure and run (PowerShell)

Set the following in the same PowerShell window you will use to run Maven. Keep the
prompt labels exactly as shown: type your email only at the `LinkedIn email:` prompt,
and your password only at the hidden password prompt. Never put either value inside
the quotation marks. Do not put real values in `.env.example` or commit them.

```powershell
$env:LINKEDIN_EMAIL = Read-Host "LinkedIn email"
$securePassword = Read-Host "LinkedIn password" -AsSecureString
$env:LINKEDIN_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$env:LINKEDIN_LIVE_TESTS = "true"
$env:LINKEDIN_SEARCH_TERM = "software testing"
mvn test
```

If the login form does not appear, the tests stop with a diagnostic instead of
trying to enter credentials. Check the browser's current page and URL manually.
LinkedIn may have redirected, changed the form, or requested a security check; do not
try to bypass CAPTCHA or verification.

`mvn test` runs the four tests in this order: valid login, invalid-password rejection,
search results, and opening the first result. Each test creates and closes its own
browser. The search tests sign in independently before searching. The negative test
submits one known invalid password attempt; do not loop it. You can optionally set
`LINKEDIN_INVALID_PASSWORD` to a non-secret, intentionally invalid value. If omitted,
the test uses a fixed invalid placeholder. A failed/blocked live attempt may prompt
LinkedIn to request additional verification. Clear credentials from the PowerShell
environment when finished:

```powershell
Remove-Item Env:LINKEDIN_EMAIL, Env:LINKEDIN_PASSWORD, Env:LINKEDIN_INVALID_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:LINKEDIN_LIVE_TESTS, Env:LINKEDIN_SEARCH_TERM -ErrorAction SilentlyContinue
```

To confirm the suite is safely gated without opening a browser, leave
`LINKEDIN_LIVE_TESTS` unset or set it to `false` and run `mvn test`; TestNG reports the
live tests as skipped.

## Run one test at a time

After setting the environment variables above, run one command at a time to execute
only that flow. The browser closes when the selected test finishes, and Maven then
returns to the terminal. By contrast, `mvn test` runs all four flows in sequence.

```powershell
mvn "-Dtest=LinkedInFlowsTest#validLoginShowsAuthenticatedPage" test
mvn "-Dtest=LinkedInFlowsTest#invalidPasswordShowsError" test
mvn "-Dtest=LinkedInFlowsTest#searchDisplaysResults" test
mvn "-Dtest=LinkedInFlowsTest#firstSearchResultOpens" test
```

## Output and evidence

- TestNG/Surefire output: `target/surefire-reports/`
- Automatic PNG screenshots for passing and failing tests: `target/screenshots/`
- Assignment report draft: [`docs/Test-Report.pdf`](docs/Test-Report.pdf)
- Report source and editable test-case table: [`docs/Test-Report.md`](docs/Test-Report.md)
- Optional PDF regeneration: `python docs/generate_report.py`

The included report intentionally marks actual outcomes and screenshots as pending;
they must be filled with evidence from your own permitted run. For the <=3-minute
recording, capture your own desktop while running the suite, hide email/password,
profile details, browser notifications, and any other personal information, and do
not record or share a CAPTCHA/security check. The recording cannot be generated
without your local account session.

## Locator and design notes

- Login uses the stable `#username`, `#password`, and `button[type=submit]` controls.
- The login flow waits for a visible, enabled sign-in submit button and clicks it;
  it reports a diagnostic if no matching button appears or no login outcome follows.
- The sign-in control is located on the page (rather than assuming it shares a form
  ancestor with the password field). It recognizes LinkedIn's sign-in button ID,
  login submit attribute, exact visible "Sign in" button text (case-insensitive),
  or accessible label.
- Search uses the visible search input's placeholder/accessible label rather than a
  generated class name.
- Search results wait for visible profile, company, school, group, or post links
  under the main content after navigation, since results render asynchronously.
  LinkedIn markup can change; update selectors only after checking the live DOM in
  DevTools.
- The first-result test waits for the destination document to finish loading and
  leaves it visible for at least two seconds before the browser closes.
- Explicit Selenium waits are used instead of fixed sleeps. Tests are sequential.
- `ScreenshotListener` captures the current browser after each passing or failing
  test; screenshots can contain personal information, so inspect and redact them
  before sharing. `mvn clean test` deletes previous screenshots because it cleans
  the entire `target/` directory first.

If page structure changes, the test may fail. Do not work around account protections;
record the limitation in the report and consult your instructor.
