import { expect, test } from '../fixtures/apiUserFixture';
import { createUser } from '../fixtures/userFactory';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';

test.describe('Login and user registration', () => {
  test.describe.configure({ timeout: 90_000 });

  test('registers a claimant and logs in with the same account', async ({ page }) => {
    const user = createUser();
    const signupPage = new SignupPage(page);
    await signupPage.open();
    await signupPage.register(user);
    await expect(page).toHaveURL(/\/login$/);

    const loginPage = new LoginPage(page);
    await loginPage.loginAs(user);
    await loginPage.expectLandingPage('user');
  });

  test('logs in with a user created through the Demo App signup API', async ({
    page,
    appUser,
  }) => {
    const user = await appUser.create();
    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs(user);
    await loginPage.expectLandingPage(user.role);
  });

  test('logs in with the preconfigured demo admin account', async ({ page }) => {
    const admin = {
      email: process.env.E2E_ADMIN_EMAIL ?? 'admin@demo.com',
      password: process.env.E2E_ADMIN_PASSWORD ?? 'Admin123!',
    };
    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs(admin);
    await loginPage.expectLandingPage('admin');
  });

  test('shows an error for invalid login credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.open();
    await loginPage.loginAs(createUser());
    await loginPage.expectError('Invalid email or password');
  });

  test('rejects registration for an email already created through the API', async ({
    page,
    appUser,
  }) => {
    const user = await appUser.create();
    const signupPage = new SignupPage(page);
    await signupPage.open();
    await signupPage.register(user);
    await signupPage.expectDuplicateEmailError();
  });
});
