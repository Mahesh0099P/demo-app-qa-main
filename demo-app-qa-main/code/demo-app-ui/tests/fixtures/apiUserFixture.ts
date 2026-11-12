import { test as base } from '@playwright/test';
import { createUser, type UserTestData } from './userFactory';

interface ApiUserFixture {
  create: (overrides?: Partial<UserTestData>) => Promise<UserTestData>;
}

interface SignupResponse {
  userId: string;
  role: string;
}

const BFF_API_URL = process.env.BFF_API_URL ?? 'http://localhost:8090';

export const test = base.extend<{ appUser: ApiUserFixture }>({
  appUser: async ({ request }, fixtureCallback) => {
    await fixtureCallback({
      async create(overrides?: Partial<UserTestData>) {
        const user = createUser(overrides);
        const response = await request.post(`${BFF_API_URL}/api/auth/signup`, {
          data: { email: user.email, password: user.password, name: user.name },
          timeout: 60_000,
        });

        if (response.status() !== 201) {
          throw new Error(
            `Demo App signup returned HTTP ${response.status()}: ${await response.text()}`,
          );
        }

        const created: SignupResponse = await response.json();
        if (!created.userId || created.role !== 'CLAIMANT') {
          throw new Error('Demo App signup did not return a CLAIMANT user with an ID');
        }

        user.id = created.userId;
        return user;
      },
    });
  },
});

export { expect } from '@playwright/test';
