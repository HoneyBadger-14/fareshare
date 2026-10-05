Here is a build order for FareShare, from an empty project to an Android release and then iPhone support. This follows the [design document](/Users/puneetkumar.gaddi/Documents/Java/Personal/Projects/FareShare/DESIGN.md).

### 1. Lock the MVP rules

- [ ] Define the first list of supported currencies. Each group selects one currency at creation.
- [ ] Define who can invite members and how long an email invitation link remains valid.
- [x] Confirm that only an expense’s **creator** can edit or reverse it.
- [x] Write examples for equal splits, exact amount splits, rounding, balances, and settlements.

### 2. Prove the infrastructure choices

- [ ] Create a Neon project with separate development and production environments.
- [ ] Test email/password and Google sign-in from Flutter through Neon Auth to a protected Spring Boot endpoint.
- [ ] Confirm session restoration, sign-out, account linking, and invalid-token handling.
- [ ] Use Firebase Authentication with Neon PostgreSQL if that integration does not work satisfactorily.
- [ ] Choose an email delivery provider and sending domain for invitation links.
- [ ] Choose where the Spring Boot API will run when testers need access outside your computer.

**Milestone:** A signed-in Android app can call a protected API, and the API can connect to PostgreSQL.

### 3. Build the backend foundation

- [x] Generate the Spring Boot **Maven** project with Web MVC, Security, Validation, Data JPA, PostgreSQL Driver, Flyway, Actuator, and Test.
- [x] Add configuration for local and production environments; keep credentials outside source control.
- [x] Create Flyway migrations for users, groups, members, invitations, expenses, shares, settlements, and activity history.
- [ ] Implement authentication and group membership checks.
- [x] Define versioned REST endpoints and consistent validation errors.

### 4. Build the accounting features

- [ ] Implement group creation, member invitations, and explicit acceptance of join links.
- [x] Implement expense creation with one payer and equal or exact amount splits.
- [x] Calculate member balances from expenses and confirmed settlements.
- [x] Generate suggested repayments without changing expense history.
- [x] Implement pending, confirmed, and rejected offline settlements.
- [x] Implement creator-only expense edits and reversals with version checks and activity history.
- [ ] Test rounding, duplicate submissions, concurrent edits, and the rule that every group’s balances sum to zero.

**Milestone:** The API can correctly manage a group’s full expense and repayment history.

### 5. Build and test the Android app

- [ ] Build sign-in, group list, group detail, invitation, add-expense, balance, activity, and settle-up screens.
- [ ] Show a split preview before saving an expense.
- [ ] Handle loading, empty, offline, validation, and expired-invitation states.
- [ ] Test on small and large Android screens and with larger text settings.
- [ ] Run an end-to-end test with several real accounts and devices.

### 6. Release the Android MVP

- [ ] Deploy the API with HTTPS and connect it to the production Neon database.
- [ ] Set up database backups, error monitoring, and a health check.
- [ ] Prepare the privacy policy, app listing, screenshots, and release build.
- [ ] Run a small tester release, fix issues, then publish.

### 7. Add iPhone support

- [ ] Build and test the same Flutter app with Xcode on iPhone.
- [ ] Verify Google sign-in, invitation links, navigation, keyboard behavior, and accessibility on iOS.
- [ ] Review [Apple’s login-services guideline](https://developer.apple.com/app-store/review/guidelines/uk/#login-services) and add a qualifying sign-in option if required for the App Store release.
- [ ] Prepare the iOS listing, tester release, and production submission.

**Finish line:** A user on either platform can join a group, add and correct expenses, see accurate balances, and confirm repayments. Mobile number sign-in with SMS OTP remains a later feature, as you decided.
