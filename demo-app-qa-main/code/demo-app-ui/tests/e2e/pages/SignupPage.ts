import { expect, type Page } from '@playwright/test';
import type { UserTestData } from '../../fixtures/userFactory';

export class SignupPage {
  constructor(private readonly page: Page) {}

  async open(): Promise<void> {
    await this.page.goto('/signup', { waitUntil: 'domcontentloaded' });
    await expect(this.page.getByRole('heading', { name: 'Create Account' })).toBeVisible();
  }

  async register(user: Pick<UserTestData, 'name' | 'email' | 'password'>): Promise<void> {
    await this.page.getByLabel('Name').fill(user.name);
    await this.page.getByLabel('Email').fill(user.email);
    await this.page.getByLabel('Password', { exact: true }).fill(user.password);
    await this.page.getByRole('button', { name: 'Sign Up', exact: true }).click();
  }

  async expectDuplicateEmailError(): Promise<void> {
    await expect(this.page.getByRole('alert')).toHaveText('Email already exists');
  }
}
