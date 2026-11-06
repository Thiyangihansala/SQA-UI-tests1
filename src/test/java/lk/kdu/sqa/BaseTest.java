package lk.kdu.sqa;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    @BeforeMethod
    public void startBrowser() {
        requireLiveTestingOptIn();
        requireEnvironmentVariable("LINKEDIN_EMAIL");
        String password = requireEnvironmentVariable("LINKEDIN_PASSWORD");
        String configuredInvalidPassword = System.getenv("LINKEDIN_INVALID_PASSWORD");
        if (configuredInvalidPassword != null && configuredInvalidPassword.equals(password)) {
            throw new SkipException("LINKEDIN_INVALID_PASSWORD must not match LINKEDIN_PASSWORD.");
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        DRIVER.set(new ChromeDriver(options));
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }

    protected WebDriver driver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("The browser has not been started for this test.");
        }
        return driver;
    }

    public static WebDriver currentDriver() {
        return DRIVER.get();
    }

    private void requireLiveTestingOptIn() {
        if (!"true".equalsIgnoreCase(System.getenv("LINKEDIN_LIVE_TESTS"))) {
            throw new SkipException("Live testing is disabled. Set LINKEDIN_LIVE_TESTS=true only after reviewing LinkedIn's terms and risks.");
        }
    }

    private String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SkipException("Set " + name + " in your environment to run this live test.");
        }
        return value;
    }
}
