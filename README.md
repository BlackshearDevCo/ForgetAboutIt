# Forget About It

A small personal Android app that reminds you about the things you keep forgetting, and
keeps nudging until you mark them done.

**One-time setup:** add the 4 signing secrets in Settings -> Secrets and variables -> Actions (names are in CLAUDE.md). Until then, pushes only run a compile check.

**Get the app:** open this repo's [Releases](../../releases/tag/latest) page on your phone,
download `ForgetAboutIt.apk`, and install it (allow installs from your browser or file
manager if Android asks). Installing a newer build over an older one keeps your reminders.

After installing, open the app, allow notifications, then open **All reminders -> Settings**
and check that every row says "On". Use "Send a test notification" to confirm delivery.

See [CLAUDE.md](CLAUDE.md) for how it is built and how reminders are scheduled.
