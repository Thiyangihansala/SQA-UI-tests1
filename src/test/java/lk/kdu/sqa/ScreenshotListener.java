package lk.kdu.sqa;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ScreenshotListener implements ITestListener {
    @Override
    public void onTestSuccess(ITestResult result) {
        saveScreenshot(result, "passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        saveScreenshot(result, "failed");
    }

    private void saveScreenshot(ITestResult result, String outcome) {
        WebDriver driver = BaseTest.currentDriver();
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return;
        }

        Path screenshotDirectory = Path.of("target", "screenshots");
        String fileName = result.getMethod().getMethodName() + "-" + outcome + "-"
                + System.currentTimeMillis() + ".png";
        try {
            Files.createDirectories(screenshotDirectory);
            Files.copy(screenshotDriver.getScreenshotAs(OutputType.FILE).toPath(),
                    screenshotDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Test screenshot saved to " + screenshotDirectory.resolve(fileName));
        } catch (IOException | WebDriverException exception) {
            System.err.println("Could not save " + outcome + " test screenshot: " + exception.getMessage());
        }
    }
}
