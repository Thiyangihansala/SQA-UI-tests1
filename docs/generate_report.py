from pathlib import Path
import textwrap


PAGE_WIDTH = 612
PAGE_HEIGHT = 792
MARGIN_X = 54
TOP_Y = 744
BOTTOM_Y = 54
LINE_HEIGHT = 15
OUTPUT = Path(__file__).with_name("Test-Report.pdf")


def pdf_escape(value):
    return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def wrap_paragraph(text, width=88):
    return textwrap.wrap(text, width=width) or [""]


def make_page(lines, page_number, total_pages):
    commands = ["BT", "/F1 10 Tf", f"{MARGIN_X} {TOP_Y} Td", f"{LINE_HEIGHT} TL"]
    for line in lines:
        commands.append(f"({pdf_escape(line)}) Tj")
        commands.append("T*")
    commands.extend([
        "ET",
        "BT",
        "/F1 8 Tf",
        f"{PAGE_WIDTH - 100} 30 Td",
        f"(Page {page_number} of {total_pages}) Tj",
        "ET",
    ])
    return "\n".join(commands).encode("ascii", errors="replace")


def make_pdf(pages):
    objects = []
    page_ids = []
    pages_id = 2
    font_id = 3
    next_id = 4
    for page_number, lines in enumerate(pages, start=1):
        stream = make_page(lines, page_number, len(pages))
        content_id = next_id
        page_id = next_id + 1
        next_id += 2
        objects.extend([
            (content_id, b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream"),
            (page_id, (
                f"<< /Type /Page /Parent {pages_id} 0 R /MediaBox [0 0 {PAGE_WIDTH} {PAGE_HEIGHT}] "
                f"/Resources << /Font << /F1 {font_id} 0 R >> >> /Contents {content_id} 0 R >>"
            ).encode()),
        ])
        page_ids.append(page_id)
    objects.extend([
        (1, b"<< /Type /Catalog /Pages 2 0 R >>"),
        (pages_id, (
            f"<< /Type /Pages /Kids [{' '.join(f'{item} 0 R' for item in page_ids)}] "
            f"/Count {len(page_ids)} >>"
        ).encode()),
        (font_id, b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
    ])
    objects.sort(key=lambda item: item[0])

    output = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = {0: 0}
    for object_id, body in objects:
        offsets[object_id] = len(output)
        output.extend(f"{object_id} 0 obj\n".encode())
        output.extend(body)
        output.extend(b"\nendobj\n")
    xref_offset = len(output)
    output.extend(f"xref\n0 {max(offsets) + 1}\n".encode())
    output.extend(b"0000000000 65535 f \n")
    for object_id in range(1, max(offsets) + 1):
        output.extend(f"{offsets[object_id]:010d} 00000 n \n".encode())
    output.extend(
        f"trailer\n<< /Size {max(offsets) + 1} /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF\n".encode()
    )
    return output


def report_pages():
    sections = [
        ("SOFTWARE QUALITY ASSURANCE - ASSIGNMENT 04", [
            "LinkedIn UI automation test report",
            "Student: [Enter your name]",
            "Programme: BSc Honours in Information Technology / Information Systems",
            "Module: IT3153 Software Quality Assurance",
            "Execution date: [Enter date]",
            "Environment: Java 21, Selenium 4.27.0, TestNG 7.10.2, Chrome [version]",
            "",
            "TEST DESIGN",
            "The suite uses TestNG and Page Object Model classes. A fresh Chrome browser is",
            "started for each test and closed afterward. Selenium explicit waits are used",
            "for controls and results. A listener saves failure screenshots under",
            "target/screenshots/. Live access requires explicit opt-in; credentials are",
            "read from environment variables and are not stored in source.",
            "",
            "TEST CASES",
            "TC-01 | Valid login | Open login; submit own email and password.",
            "Expected: feed or authenticated navigation is visible.",
            "Actual: Passed - user confirmed successful login during the live run.",
            "Result: Pass.",
            "",
            "TC-02 | Invalid login | Submit own email and one intentionally invalid password.",
            "Expected: a visible login error appears.",
            "Actual: Wrong-password UI appeared, but automation assertion timed out.",
            "Error detection updated; rerun once to verify. Result: Pending.",
            "",
            "TC-03 | Search | Sign in; submit configured search phrase.",
            "Expected: search results URL and at least one visible result.",
            "Actual: Passed - user confirmed navigation to software testing results.",
            "Result: Pass.",
            "",
            "TC-04 | Open first result | Sign in; search; click first visible result.",
            "Expected: current URL matches the clicked result path.",
            "Actual: Passed 2026-10-30. Maven: 1 test, 0 failures, 0 errors, 0 skipped.",
            "The browser opened a profile result. Result: Pass.",
        ]),
        ("LOCATORS AND LIMITATIONS", [
            "Login fields use #username and #password; the submit button uses",
            "button[type='submit']. Search uses the visible placeholder/accessible label.",
            "Results use LinkedIn result-container selectors. These selectors can become",
            "stale when LinkedIn changes its markup; validate them in DevTools.",
            "",
            "SECURITY AND ACCOUNT HANDLING",
            "Credentials are read only from process environment variables. Live tests do",
            "not run without LINKEDIN_LIVE_TESTS=true. The negative test makes one attempt",
            "with an intentionally invalid value. Repeated attempts could trigger account",
            "protections. No password is included in this report.",
            "",
            "LinkedIn may change its interface, require additional login steps, show a",
            "CAPTCHA/security checkpoint, or limit automated activity. The suite stops",
            "when common challenge URLs are detected and does not attempt to bypass them.",
            "Document any challenge and follow instructor/platform guidance.",
            "",
            "Challenges actually encountered: [Complete after execution.]",
            "How handled: [Complete after execution; never claim a challenge was bypassed.]",
            "",
            "UI automation cannot cover all account configurations, locales, or future",
            "LinkedIn releases. A successful run only describes the observed run. Obtain",
            "permission and keep live runs to a minimum.",
        ]),
        ("EXECUTION EVIDENCE AND REFLECTION", [
            "Run command: mvn test",
            "TC-01, TC-03, and TC-04 were reported/observed as passing.",
            "TC-02 showed the expected UI but its automated assertion failed; rerun once.",
            "TC-04 terminal: 1 test, 0 failures, 0 errors, 0 skipped (2026-10-30).",
            "Screenshot evidence: [Insert a redacted run or failure screenshot.]",
            "Recording: [Record your own <=3-minute run; hide credentials and personal data.]",
            "",
            "This project is a prepared assignment implementation, not evidence that the",
            "live tests have already run. No account credentials, test outcomes, screenshots,",
            "or screen recording were supplied or fabricated. Complete these fields after",
            "an authorized local run.",
            "",
            "REFLECTION",
            "A third-party site is not a stable test environment under the student's control.",
            "Selectors and behavior can change without notice; authentication controls and",
            "anti-automation measures may make tests unavailable. Tests must stop rather",
            "than circumvent those protections. Prefer a local/mock application for",
            "repeatable regression testing and use the live site only as allowed by its",
            "terms and the course instructions.",
            "",
            "Implementation: Java 17+ | Selenium WebDriver | TestNG | Chrome",
        ]),
    ]
    pages = []
    for title, content in sections:
        lines = []
        for line in [title, *content]:
            if line == "":
                lines.append("")
            else:
                lines.extend(wrap_paragraph(line))
        pages.append(lines)
    return pages


if __name__ == "__main__":
    OUTPUT.write_bytes(make_pdf(report_pages()))
    print(f"Generated {OUTPUT}")
