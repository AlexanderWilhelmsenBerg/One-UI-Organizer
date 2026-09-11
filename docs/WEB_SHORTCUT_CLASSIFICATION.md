# Web Shortcut classification decision

**Lane:** Agent 63 / `feature/classification-web-shortcuts`

This note records the supported metadata reviewed for browser-created launcher entries and the deliberately narrow production rule selected for the post-v0.1 classification wave.

The private owner-device classification report remains private. No raw launcher row, label, generated package suffix, or full device inventory is committed here.

## Supported metadata investigated

The existing app-owned `InstalledApp` model already carries the supported Android-visible identity needed for the proven case:

- package identity through `AppId.packageName`;
- exact launcher component through `LaunchTargetId`;
- display label;
- app-owned Android-declared application category.

No new `InstalledApp` field or platform scanner signal is required for the implemented rule.

The owner-device evidence reviewed by the classification foundation contains one generated Chromium WebAPK launcher target under the `org.chromium.webapk.` namespace. The foundation deliberately added an evidence-backed package-prefix selector seam for generated identities such as this one.

Chromium's current Web App Identifiers documentation independently documents WebAPK Android package names as `org.chromium.webapk.<hash>`:

- https://chromium.googlesource.com/chromium/src/+/main/components/webapps/docs/identifiers.md

That same upstream document explains why generic TWA inference is different: standard TWAs use developer-chosen Android package names. It also documents the narrower auto-minted TWA form `com.android.webapp.<hash>`, but the reviewed owner-device evidence has not established a confirmed reusable match for that namespace in this lane. The frozen classification roadmap requires separate evidence before widening TWA classification, so this branch does not add a TWA rule.

No supported package/component signature was established here for Samsung Internet-created shortcuts or other browser shortcut implementations. They therefore remain on the ordinary known-rule / Android category / `Unsorted` path.

## Production decision

Classify packages whose Android package identity starts with the segment-bounded prefix:

```text
org.chromium.webapk.
```

as `AppCategory.WEB_SHORTCUTS`.

The rule uses only package identity. It does not inspect or infer from:

- display labels;
- URLs;
- activity/class names;
- browser names;
- Samsung launcher data;
- private launcher APIs.

Ordinary browser packages such as Chrome and Samsung Internet do not match the WebAPK namespace and are not classified as Web Shortcuts by this rule.

## False-positive boundary

The remaining false-positive risk is a package that deliberately uses the Chromium-generated `org.chromium.webapk.*` namespace without actually being a WebAPK. The risk is considered low enough for this narrow rule because both the owner-device evidence and Chromium's maintained documentation identify the namespace as generated WebAPK identity.

The rule intentionally does not broaden to `org.chromium.*`, class-name fragments, website-looking labels, standard TWA wrappers, or generic browser packages.

## Private owner-device effect

Against the same 566-target foundation report, before any parallel general/game rule expansion:

| Aggregate | Before | After Agent 63 |
| --- | ---: | ---: |
| `WEB_SHORTCUTS` | 0 | 1 |
| `UNSORTED` | 225 | 224 |
| `KNOWN_APP_RULE` source | 5 | 6 |
| `UNSORTED_FALLBACK` source | 225 | 224 |
| Total launcher targets | 566 | 566 |

Exactly one reviewed launcher target matches the implemented WebAPK prefix in that evidence set. No raw target identity is included in this document.

## Contract and platform impact

- No shared model/contract change.
- No platform scanner change.
- No persistence change or migration.
- No taxonomy change.
- No dependency/toolchain change.
- No new permission.
- `INTERNET` remains absent.
- `QUERY_ALL_PACKAGES` remains absent.

User override precedence remains unchanged: a manual category override still wins before the bundled WebAPK rule.
