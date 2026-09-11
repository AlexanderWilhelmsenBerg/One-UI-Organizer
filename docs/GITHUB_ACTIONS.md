# GitHub Actions and Android release signing

This document describes the repository's GitHub Actions workflows, APK artifact behavior, and the distribution-signing setup used by One UI Organizer.

Exact Android/Kotlin/Gradle/toolchain versions remain authoritative in [STABLE_BASELINE.md](STABLE_BASELINE.md). This document does not duplicate that version inventory.

## Workflow inventory

### CI

Path: `.github/workflows/ci.yml`

Triggers:

- pull requests;
- pushes to `main`.

Purpose:

- compile the debug app;
- run unit tests, Android Lint, ktlint, and dependency health;
- enforce strict dependency verification;
- prove configuration-cache reuse;
- reject forbidden v0.1 permissions.

CI is the permanent quality gate. The APK workflow below is a developer/distribution convenience and does not replace CI.

### Build APK

Path: `.github/workflows/build-apk.yml`

Trigger: manual `workflow_dispatch`.

Input:

- `variant=debug` (default);
- `variant=release`.

Both jobs use the repository Gradle wrapper, strict dependency verification, the committed Gradle daemon JVM criteria, and the same API 37.0 provisioning strategy as permanent CI.

The jobs remain separate because they produce different Android build types, but both manual artifact jobs intentionally use the same long-lived distribution key from the GitHub `release` environment. This lets a manually downloaded debug APK update a previously installed release APK, or vice versa, without a signing-certificate mismatch.

Ordinary local debug builds are not changed by this policy. When signing environment variables are absent, Android's normal local debug signing remains in effect.

#### Debug

Runs `:app:assembleDebug` with the GitHub `release` environment signing credentials. The workflow reconstructs the keystore only in runner-temporary storage, passes the temporary path/password to Gradle, disables persistent Gradle Action caching for that job, and verifies the generated APK with `apksigner`.

The resulting APK remains a debuggable Android debug build; only its signing certificate is shared with release builds. It is renamed to:

```text
OneUIOrganizer-debug-<short-sha>.apk
```

and uploaded with `actions/upload-artifact` direct single-file mode (`archive: false`). The workflow run therefore exposes the APK itself rather than an artifact ZIP wrapper.

The job summary reports the variant, full commit SHA, filename, byte/IEC size, SHA-256 digest, signature information from the verification step, and the artifact URL returned by the upload action.

Because this job uses the long-lived project signing key, it is manual-only and attached to the protected `release` environment rather than running on pull requests.

#### Release

The release job is also attached to the protected GitHub environment named `release`. It reconstructs the signing keystore only in the ephemeral runner's temporary directory, exposes only that temporary path plus the password to Gradle, builds `:app:assembleRelease`, and verifies the produced APK with `apksigner verify --verbose --print-certs`.

The final file is renamed to:

```text
OneUIOrganizer-release-<short-sha>.apk
```

and uploaded in the same direct single-file mode.

Both debug and release artifact jobs are expected to fail early when their required environment secrets are absent.

## Artifact retention and distribution model

Build APK workflow artifacts are retained for **14 days**. They are convenience outputs for development and testing, not the permanent release channel. Fourteen days is long enough to retrieve recent manually requested builds while limiting repository artifact storage growth.

There are three different GitHub binary-delivery concepts:

1. Traditional workflow artifacts are archived by the artifact service; historically a single APK was commonly downloaded inside a ZIP.
2. `actions/upload-artifact` v7 supports direct upload of exactly one file with `archive: false`. In that mode the file is not zipped, the `name` input is ignored, and the uploaded file's own filename becomes the artifact name. This is what Build APK uses.
3. GitHub Release assets are durable files attached to a release/tag. Formal releases should eventually attach the signed APK and its `.sha256` file as Release assets instead of relying on expiring workflow artifacts.

## Distribution-signing design

The distribution key is project infrastructure, not source code.

The repository uses the constant alias:

```text
oneui-organizer-upload
```

Distribution signing is enabled in `app/build.gradle.kts` only when both of these environment values are present:

```text
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
```

When both are present, the same signing configuration is assigned to the Android `debug` and `release` build types. This is used by the manual GitHub Build APK workflow so both downloadable variants have one stable application-signing identity.

If those environment values are absent, local debug/test/lint/verification tasks remain usable without signing credentials and local debug builds continue using Android's normal debug key. The GitHub Build APK jobs supply `ANDROID_KEYSTORE_PATH` only after reconstructing the secret keystore into runner-temporary storage.

The long-lived private keystore must never be committed. `.gitignore` already excludes `*.jks`, `*.keystore`, and `keystore.properties`, but the preferred policy is stronger: keep the original keystore outside the repository checkout entirely.

When Google Play distribution is introduced, this locally controlled key can be used as the Play **upload key** while Google Play App Signing protects the app-signing key used for store-distributed APKs. Play publishing is intentionally not automated yet.

## Create the upload keystore on Windows PowerShell

First confirm that a JDK-provided `keytool` is available:

```powershell
Get-Command keytool
keytool -version
```

Choose a location outside the Git checkout. The recommended user-local location is:

```powershell
$KeyStorePath = Join-Path $HOME ".android\one-ui-organizer-upload.jks"
$KeyAlias = "oneui-organizer-upload"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $KeyStorePath) | Out-Null
```

Create an RSA 4096-bit key with a 100-year validity. Omitting `-storepass`, `-keypass`, and `-dname` is intentional: `keytool` prompts interactively, so passwords and certificate identity fields do not enter PowerShell history.

```powershell
keytool -genkeypair `
  -v `
  -keystore $KeyStorePath `
  -storetype JKS `
  -alias $KeyAlias `
  -keyalg RSA `
  -keysize 4096 `
  -sigalg SHA256withRSA `
  -validity 36500
```

At the key-password prompt, press Enter to reuse the keystore password. Android's current app-signing guidance recommends the key password match the keystore password for tooling compatibility.

`keytool` will ask for certificate identity fields rather than this repository inventing them. Typical prompts represent:

- CN: certificate owner/common name;
- OU: organizational unit;
- O: organization;
- L: locality/city;
- ST: state/province/region;
- C: two-letter country code.

Enter values appropriate for the publisher identity you want embedded in the public certificate.

Verify the keystore and inspect the certificate fingerprints:

```powershell
keytool -list -v -keystore $KeyStorePath -alias $KeyAlias
```

Look for the `SHA256:` certificate fingerprint and preserve it in a non-secret project/release record if useful. The fingerprint and public certificate are safe to share; the JKS private key and passwords are not.

Back up the JKS securely in more than one trusted location and retain its password in a password manager or similarly durable secret store. Outside Play App Signing, loss of the signing key prevents shipping updates that Android accepts as the same application. Even when this key becomes a Play upload key and is resettable through Play, it remains sensitive project infrastructure.

Do not move or copy the JKS into the One UI Organizer checkout.

## Configure the GitHub `release` environment and secrets from PowerShell

GitHub environment secrets are preferred because only the manually invoked signed-artifact jobs reference the `release` environment. Permanent pull-request/main CI does not receive the signing material. Repository-level secrets would be available to a broader set of workflows/jobs and should be a fallback only when environments are not suitable.

Verify GitHub CLI first:

```powershell
gh --version
gh auth status
```

Set reusable variables for the commands below:

```powershell
$Repo = "AlexanderWilhelmsenBerg/One-UI-Organizer"
$ReleaseEnvironment = "release"
$KeyStorePath = Join-Path $HOME ".android\one-ui-organizer-upload.jks"
```

Create/configure the `release` environment in GitHub repository Settings > Environments if it does not already exist. Add required reviewers or deployment protections there if desired.

Encode the binary JKS only in memory and pipe the Base64 text directly to GitHub CLI. This does not create a persistent Base64 file and does not print the Base64 value:

```powershell
$bytes = [System.IO.File]::ReadAllBytes($KeyStorePath)
$base64 = [Convert]::ToBase64String($bytes)
$base64 | gh secret set ANDROID_KEYSTORE_BASE64 --env $ReleaseEnvironment --repo $Repo
Remove-Variable base64, bytes
```

Set the password interactively so it is not placed on the command line or in PowerShell history:

```powershell
gh secret set ANDROID_KEYSTORE_PASSWORD --env $ReleaseEnvironment --repo $Repo
```

GitHub CLI prompts locally for the secret value when no `--body` value or redirected standard input is supplied.

Verify that the expected secret **names** exist. GitHub will not reveal their values:

```powershell
gh secret list --env $ReleaseEnvironment --repo $Repo
```

Expected names:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
```

A separate `ANDROID_KEY_PASSWORD` is intentionally not used because the documented setup creates the key with the same password as the keystore. If the key is ever migrated to a keystore with a different key password, the Gradle/workflow contract must be deliberately updated rather than silently adding another credential.

Never paste the keystore Base64 text, private key, or passwords into an issue, pull request, workflow log, or chat for verification.

## Manual workflow use

Once the workflow exists on the repository's default branch, a developer can run it from the Actions UI or GitHub CLI.

Signed debug:

```powershell
gh workflow run build-apk.yml --repo AlexanderWilhelmsenBerg/One-UI-Organizer --ref main -f variant=debug
```

Signed release:

```powershell
gh workflow run build-apk.yml --repo AlexanderWilhelmsenBerg/One-UI-Organizer --ref main -f variant=release
```

Both variants require the `release` environment secrets and use the same long-lived certificate. After the first transition installation, switching between these GitHub-hosted debug and release APKs does not require uninstalling solely because of signing identity. Android's normal version-code/install compatibility rules still apply.

Workflow-dispatch events require the workflow file to exist on the default branch. During PR development, a temporary branch-only push trigger may be used solely to prove the workflow on GitHub Actions; that trigger must be removed before the PR is declared ready.

## Future formal GitHub Release workflow

When the project is ready for tagged releases, add a separate release workflow for tags such as `v0.1.0`. Keep it distinct from Build APK.

The release workflow should:

- validate that the tag/version relationship is intentional;
- run the release-quality verification lane;
- build and verify a signed release APK;
- calculate SHA-256 and create a matching `.sha256` file;
- use GitHub's built-in `gh release create` / `gh release upload` path where practical;
- attach `OneUIOrganizer-v0.1.0.apk` and `OneUIOrganizer-v0.1.0.apk.sha256` directly as Release assets;
- use only the minimum write permission required to create the GitHub Release;
- optionally add an AAB only when Google Play distribution becomes a real requirement.

Do not automate Play Store publishing until explicitly requested.

## Smallest useful long-term workflow set

Keep these responsibilities separate:

- **CI** — keep and improve the existing PR/main quality gate; do not duplicate it and do not expose distribution-signing secrets to it.
- **Build APK** — manual signed debug/release convenience builds; direct short-lived APK artifacts; signing secrets only in the manual jobs through the `release` environment.
- **GitHub Release** — add when version/tag/release policy is ready; durable signed assets and checksum.
- **Instrumentation/device CI** — add only when meaningful emulator/managed-device tests exist. Samsung-specific acceptance remains physical-device/manual.
- **Dependency freshness** — a scheduled/manual advisory `dependencyUpdates` workflow can be added when the project wants automatic update reporting; it must not auto-apply upgrades or bypass compatibility review.
- **Security scanning** — evaluate CodeQL/dependency review once the feature graph settles and repository settings/licensing provide useful signal. Avoid adding noisy duplicate scanners merely for workflow count.
- **Performance/benchmark** — defer until benchmark modules and stable end-to-end journeys exist, as required by the engineering baseline.

The current repository is small enough that CI plus Build APK is the useful implemented set. Release, instrumentation, security, dependency-freshness, and performance workflows should be introduced only when their corresponding product/development need is real.
