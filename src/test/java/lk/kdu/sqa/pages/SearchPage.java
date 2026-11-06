package lk.kdu.sqa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.time.Duration;

public class SearchPage {
    private static final By SEARCH_INPUT = By.cssSelector(
            "input[placeholder*='Search'], input[aria-label*='Search']");
    private static final By RESULT_LINKS = By.cssSelector(
            "main a[href*='/in/'], main a[href*='/company/'], main a[href*='/school/'], "
                    + "main a[href*='/showcase/'], main a[href*='/groups/'], main a[href*='/posts/']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public SearchPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void search(String term) {
        WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(SEARCH_INPUT));
        searchBox.click();
        searchBox.clear();
        searchBox.sendKeys(term, Keys.ENTER);
        wait.until(currentDriver -> hasResultsPage() || isChallengePage(currentDriver.getCurrentUrl()));
        stopIfSecurityChallenge();
        try {
            wait.until(currentDriver -> hasVisibleResult() || isChallengePage(currentDriver.getCurrentUrl()));
        } catch (org.openqa.selenium.TimeoutException exception) {
            throw new IllegalStateException(
                    "LinkedIn opened the search URL but no visible profile, company, school, group, or post result link appeared. "
                            + "Check the page manually for an empty result set, changed markup, or a security prompt.",
                    exception);
        }
        stopIfSecurityChallenge();
    }

    public boolean hasResultsPage() {
        return driver.getCurrentUrl().contains("/search/results/");
    }

    public boolean hasVisibleResult() {
        return driver.findElements(RESULT_LINKS).stream()
                .anyMatch(link -> {
                    try {
                        String href = link.getAttribute("href");
                        return link.isDisplayed() && href != null && isResultUrl(href);
                    } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                        return false;
                    }
                });
    }

    public String openFirstResult() {
        WebElement firstResultLink = wait.until(currentDriver -> currentDriver.findElements(RESULT_LINKS).stream()
                .filter(link -> {
                    try {
                        return link.isDisplayed()
                                && isResultUrl(link.getAttribute("href"))
                                && !link.getText().isBlank();
                    } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null));
        String resultUrl = firstResultLink.getAttribute("href");
        String expectedPath = URI.create(resultUrl).getPath();
        firstResultLink.click();
        wait.until(currentDriver -> currentDriver.getCurrentUrl().contains(expectedPath)
                || isChallengePage(currentDriver.getCurrentUrl()));
        stopIfSecurityChallenge();
        wait.until(currentDriver -> currentDriver.getCurrentUrl().contains(expectedPath)
                && "complete".equals(((JavascriptExecutor) currentDriver)
                .executeScript("return document.readyState")));
        keepResultPageVisible(Duration.ofSeconds(2));
        return expectedPath;
    }

    private boolean isResultUrl(String href) {
        return href != null && (href.contains("/in/")
                || href.contains("/company/")
                || href.contains("/school/")
                || href.contains("/showcase/")
                || href.contains("/groups/")
                || href.contains("/posts/"));
    }

    private void keepResultPageVisible(Duration duration) {
        long visibleUntil = System.nanoTime() + duration.toNanos();
        wait.until(currentDriver -> System.nanoTime() >= visibleUntil);
    }

    public boolean isOnOpenedResult(String expectedPath) {
        return driver.getCurrentUrl().contains(expectedPath) && !hasResultsPage();
    }

    private void stopIfSecurityChallenge() {
        if (isChallengePage(driver.getCurrentUrl())) {
            throw new IllegalStateException(
                    "LinkedIn presented a CAPTCHA or security verification. The test stopped; do not attempt to bypass it.");
        }
    }

    private boolean isChallengePage(String url) {
        String normalizedUrl = url.toLowerCase();
        return normalizedUrl.contains("checkpoint") || normalizedUrl.contains("challenge") || normalizedUrl.contains("captcha");
    }
}
