package lk.kdu.sqa;

import lk.kdu.sqa.pages.LoginPage;
import lk.kdu.sqa.pages.SearchPage;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(ScreenshotListener.class)
public class LinkedInFlowsTest extends BaseTest {
    private static final String INVALID_PASSWORD_FALLBACK = "DefinitelyNotARealPassword-Invalid-Test";

    @Test(priority = 1, description = "A valid account can sign in")
    public void validLoginShowsAuthenticatedPage() {
        String email = requiredEnvironmentVariable("LINKEDIN_EMAIL");
        String password = requiredEnvironmentVariable("LINKEDIN_PASSWORD");

        LoginPage loginPage = new LoginPage(driver());
        loginPage.open();
        loginPage.signIn(email, password);

        Assert.assertTrue(loginPage.isAuthenticated(),
                "Expected the feed or authenticated navigation to appear after sign-in.");
    }

    @Test(priority = 2, description = "An invalid password is rejected")
    public void invalidPasswordShowsError() {
        String email = requiredEnvironmentVariable("LINKEDIN_EMAIL");
        String invalidPassword = System.getenv("LINKEDIN_INVALID_PASSWORD");
        if (invalidPassword == null || invalidPassword.isBlank()) {
            invalidPassword = INVALID_PASSWORD_FALLBACK;
        }
        Assert.assertNotEquals(invalidPassword, requiredEnvironmentVariable("LINKEDIN_PASSWORD"),
                "The negative test password must not match the real account password.");

        LoginPage loginPage = new LoginPage(driver());
        loginPage.open();
        loginPage.signIn(email, invalidPassword);

        Assert.assertTrue(loginPage.hasLoginError(),
                "Expected LinkedIn to show a login error for the intentionally invalid password.");
    }

    @Test(priority = 3, description = "Search displays at least one result")
    public void searchDisplaysResults() {
        signInWithConfiguredAccount();

        SearchPage searchPage = new SearchPage(driver());
        searchPage.search(searchTerm());

        Assert.assertTrue(searchPage.hasResultsPage(), "Expected the search results URL.");
        Assert.assertTrue(searchPage.hasVisibleResult(), "Expected at least one visible search result.");
    }

    @Test(priority = 4, description = "Opening the first result navigates to that result")
    public void firstSearchResultOpens() {
        signInWithConfiguredAccount();

        SearchPage searchPage = new SearchPage(driver());
        searchPage.search(searchTerm());
        Assert.assertTrue(searchPage.hasVisibleResult(), "Cannot open a result when no result is visible.");

        String expectedPath = searchPage.openFirstResult();
        Assert.assertTrue(searchPage.isOnOpenedResult(expectedPath),
                "Expected the opened page to match the first result URL path: " + expectedPath);
    }

    private void signInWithConfiguredAccount() {
        String email = requiredEnvironmentVariable("LINKEDIN_EMAIL");
        String password = requiredEnvironmentVariable("LINKEDIN_PASSWORD");
        LoginPage loginPage = new LoginPage(driver());
        loginPage.open();
        loginPage.signIn(email, password);
        Assert.assertTrue(loginPage.isAuthenticated(), "Search requires a successful sign-in first.");
    }

    private String searchTerm() {
        String value = System.getenv("LINKEDIN_SEARCH_TERM");
        return value == null || value.isBlank() ? "software testing" : value;
    }

    private String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SkipException("Set " + name + " in your environment to run this live test.");
        }
        return value;
    }
}
