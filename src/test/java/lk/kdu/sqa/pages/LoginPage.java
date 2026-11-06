package lk.kdu.sqa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage {
    private static final By USERNAME = By.cssSelector(
            "#username, input[name='session_key'], input[autocomplete='username'], input[type='email']");
    private static final By PASSWORD = By.cssSelector(
            "#password, input[name='session_password'], input[autocomplete='current-password'], input[type='password']");
    private static final By SIGN_IN_CONTROLS = By.xpath(
            "//*[@id='sign-in-form__submit' "
                    + "or @data-litms-control-urn='login-submit' "
                    + "or (self::button and translate(normalize-space(string(.)), "
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='sign in') "
                    + "or (self::input and @type='submit' and translate(normalize-space(@value), "
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='sign in') "
                    + "or (self::button and @aria-label and translate(normalize-space(@aria-label), "
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='sign in') "
                    + "or (@role='button' and translate(normalize-space(string(.)), "
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='sign in')]");
    private static final By LOGIN_ERROR = By.cssSelector(
            "#error-for-password, #error-for-username, .alert-content, .form__label--error, "
                    + ".artdeco-inline-feedback, [role='alert'], [class*='error']");
    private static final By GLOBAL_NAV = By.cssSelector("#global-nav, nav.global-nav");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void open() {
        driver.get("https://www.linkedin.com/login");
        try {
            wait.until(currentDriver -> isChallengePage(currentDriver.getCurrentUrl())
                    || hasVisibleEnabledField(USERNAME) && hasVisibleEnabledField(PASSWORD));
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "The LinkedIn login form (both email and password fields) did not appear at "
                            + driver.getCurrentUrl()
                            + ". Check the page manually for a changed layout, redirect, or security check; do not bypass verification.",
                    exception);
        }
        stopIfSecurityChallenge();
    }

    public void signIn(String email, String password) {
        WebElement usernameField;
        WebElement passwordField;
        try {
            usernameField = findVisible(USERNAME);
            passwordField = findVisible(PASSWORD);
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "LinkedIn's login page did not expose both visible email and password fields at "
                            + driver.getCurrentUrl()
                            + ". Check the page manually for a changed form or security verification; do not bypass it.",
                    exception);
        }

        usernameField.clear();
        usernameField.sendKeys(email);
        passwordField.clear();
        passwordField.sendKeys(password);
        wait.until(currentDriver -> email.equals(usernameField.getDomProperty("value"))
                && password.equals(passwordField.getDomProperty("value")));

        clickSignIn();

        try {
            wait.until(currentDriver -> isChallengePage(currentDriver.getCurrentUrl())
                    || hasLoginError() || isAuthenticated());
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "The sign-in button was clicked, but LinkedIn did not show an authenticated page, "
                            + "a login error, or a detectable security challenge. Check the browser manually "
                            + "and do not bypass verification.",
                    exception);
        }
        stopIfSecurityChallenge();
    }

    public boolean isAuthenticated() {
        String url = driver.getCurrentUrl();
        if (url.contains("/feed")) {
            return true;
        }
        return !driver.findElements(GLOBAL_NAV).isEmpty()
                && !driver.findElements(By.cssSelector("input[placeholder*='Search'], input[aria-label*='Search']")).isEmpty();
    }

    public boolean hasLoginError() {
        List<WebElement> errors = driver.findElements(LOGIN_ERROR);
        if (errors.stream().anyMatch(element -> {
            try {
                return element.isDisplayed() && !element.getText().isBlank();
            } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                return false;
            }
        })) {
            return true;
        }

        WebElement body = driver.findElement(By.tagName("body"));
        String visibleText = body.getText().toLowerCase().replaceAll("\\s+", " ");
        return visibleText.contains("wrong password")
                || visibleText.contains("incorrect password")
                || visibleText.contains("invalid password")
                || visibleText.contains("password is incorrect")
                || visibleText.contains("password you entered is incorrect")
                || visibleText.contains("email or password")
                || visibleText.contains("email address or password")
                || visibleText.contains("not the right password");
    }

    private void stopIfSecurityChallenge() {
        if (isChallengePage(driver.getCurrentUrl())) {
            throw new IllegalStateException(
                    "LinkedIn presented a CAPTCHA or security verification. The test stopped; complete it manually if appropriate, then rerun.");
        }
    }

    private boolean isChallengePage(String url) {
        String normalizedUrl = url.toLowerCase();
        return normalizedUrl.contains("checkpoint") || normalizedUrl.contains("challenge") || normalizedUrl.contains("captcha");
    }

    private WebElement findVisible(By locator) {
        return wait.until(currentDriver -> currentDriver.findElements(locator).stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .findFirst()
                .orElse(null));
    }

    private boolean hasVisibleEnabledField(By locator) {
        return driver.findElements(locator).stream()
                .anyMatch(element -> element.isDisplayed() && element.isEnabled());
    }

    private void clickSignIn() {
        try {
            WebElement signInButton = wait.until(currentDriver -> {
                List<WebElement> controls = currentDriver.findElements(SIGN_IN_CONTROLS).stream()
                        .filter(WebElement::isDisplayed)
                        .filter(WebElement::isEnabled)
                        .toList();

                WebElement explicitlyIdentified = controls.stream()
                        .filter(this::isExplicitSignInControl)
                        .findFirst()
                        .orElse(null);
                if (explicitlyIdentified != null) {
                    return explicitlyIdentified;
                }

                List<WebElement> submitControls = controls.stream()
                        .filter(this::isSubmitControl)
                        .toList();
                return submitControls.size() == 1 ? submitControls.get(0) : null;
            });
            signInButton.click();
        } catch (TimeoutException exception) {
            throw new IllegalStateException(
                    "The login page did not expose one unambiguous, visible and enabled Sign in button. "
                            + "Check the page manually for a changed layout or account/security prompt.",
                    exception);
        }
    }

    private boolean isExplicitSignInControl(WebElement control) {
        String id = control.getAttribute("id");
        String label = control.getAttribute("aria-label");
        String text = control.getText();
        String value = control.getAttribute("value");
        return "sign-in-form__submit".equals(id)
                || "Sign in".equalsIgnoreCase(label == null ? "" : label.trim())
                || "Sign in".equalsIgnoreCase(text == null ? "" : text.trim())
                || "Sign in".equalsIgnoreCase(value == null ? "" : value.trim());
    }

    private boolean isSubmitControl(WebElement control) {
        String type = control.getAttribute("type");
        return "submit".equalsIgnoreCase(type);
    }
}
