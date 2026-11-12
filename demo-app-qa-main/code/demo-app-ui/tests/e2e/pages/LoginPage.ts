import { expect, type Page } from '@playwright/test';
import type { UserTestData } from '../../fixtures/userFactory';

export class LoginPage {
  constructor(private readonly page: Page) {}

  async open(): Promise<void> {
    await this.page.goto('/login', { waitUntil: 'domcontentloaded' });
    await expect(this.page.getByRole('heading', { name: 'Log In' })).toBeVisible();
  }

  async loginAs(user: Pick<UserTestData, 'email' | 'password'>): Promise<void> {
    await this.page.getByLabel('Email').fill(user.email);
    await this.page.getByLabel('Password', { exact: true }).fill(user.password);
    await this.page.getByRole('button', { name: 'Log In', exact: true }).click();
  }

  async expectLandingPage(role: UserTestData['role']): Promise<void> {
    const destination =
      role === 'admin'
        ? /\/admin\/dashboard(?:[?#].*)?$/
        : /\/claims(?:[?#].*)?$/;
    await expect(this.page).toHaveURL(destination);
  }

  async expectError(message: string): Promise<void> {
    await expect(this.page.getByRole('alert')).toHaveText(message);
  }
}
