# Supported Metadata Enrichment

## Status

Agent 100 implements the first supported external metadata-enrichment path in PR #23, now merged to `main` at Agent-102 start baseline `8d34cc88106bb346baac00c012f9ad12c7a9e50f`. This document describes the merged provider/cache/classification contract. Combined metadata + presentation acceptance remains owned by Agent 102 and physical Samsung acceptance must be recorded separately rather than inferred from CI.

The existing planning contract in `PLAN.md`, `MOSCOW.md`, `ACCEPTANCE_CRITERIA.md`, `PARALLEL_DEVELOPMENT.md`, `CLASSIFICATION_ROADMAP.md`, and the category-management/backup documentation was reviewed before implementation. The first-rollout policy remains deliberately conservative.

## Provider

The first and only provider is F-Droid.

The implementation uses F-Droid's documented repository index v2:

- API documentation: <https://f-droid.org/docs/All_our_APIs/>
- repository index: <https://f-droid.org/repo/index-v2.json>
- signed index entry metadata: <https://f-droid.org/repo/entry.json>

F-Droid was selected because the repository index is a supported, documented public mechanism and exposes package metadata/categories for the subset of apps represented there. The implementation does not scrape Google Play and does not call undocumented/private store endpoints.

On 2026-09-13, F-Droid's signed `entry.json` advertised an `index-v2.json` size of roughly 55.2 MB and a 14-day `maxAge`. That makes the index inappropriate for first-render blocking work. The provider therefore runs only after the local installed-app scan has been published and is protected by a bounded response limit.

No additional provider is added in this wave. Coverage expansion requires separate evidence and product review rather than accumulating integrations by default.

## Boundary and app-owned contracts

The dependency direction is:

```text
F-Droid index-v2.json
        |
HttpFdroidIndexDocumentSource
        |
FdroidMetadataProvider
        |
SupportedAppMetadataProvider
        |
SupportedAppMetadata
        |
SupportedMetadataCategoryMapper
        |
CategoryEngine
```

Provider/network/JSON types remain inside the platform/data adapter boundary. Domain, repository and UI contracts do not expose F-Droid JSON objects, HTTP response objects or provider-specific category enums.

The app-owned metadata model stores only:

- package identity (`AppId`);
- supported provider identity;
- provider categories useful as classification evidence;
- fetch timestamp.

It intentionally does not model descriptions, screenshots, versions, links, donations, licenses or other provider data that Organizer does not need.

## Provider result contract

`SupportedAppMetadataProvider.lookup()` accepts a set of app package identities and returns an explicit batch result:

- success with metadata by package plus explicit package misses; or
- failure.

Provider exceptions do not become domain/UI state. Coroutine cancellation remains cancellation; ordinary network, HTTP, parse and malformed-document failures become a provider failure.

The F-Droid adapter performs one bulk index request rather than one HTTP request per installed app.

## Classification precedence

The first-rollout precedence is:

```text
user override
    > bundled deterministic rule
    > Android-declared category
    > supported metadata
    > Unsorted
```

This is intentionally more conservative than placing metadata above Android's declared category. It matches the existing post-PR-21 product plan and means supported metadata only improves apps that would otherwise remain `Unsorted`.

When metadata determines the category, `ClassificationSource.SUPPORTED_METADATA` is recorded. Existing classification explanations and the local classification report can therefore distinguish metadata decisions from user, rule-pack, Android and fallback decisions.

Metadata can only produce an existing built-in `AppCategory`. It cannot create or rename a custom category, modify category order, or replace a custom-category assignment.

## F-Droid category mapping

Provider category strings are never used as organizer category identity. `SupportedMetadataCategoryMapper` contains an explicit mapping table into existing built-in categories.

Mapped evidence currently includes conservative groups such as:

- development -> Development;
- games / educational game -> Games;
- email / messaging / phone & SMS / contact -> Communication;
- forum -> Social;
- finance manager / money -> Finance;
- shopping -> Shopping;
- navigation / public transport / location tracker & sharer -> Travel & Navigation;
- music practice tool / podcast / radio -> Music & Audio;
- gallery -> Photos;
- reading / ebook reader / news / bookmark -> Reading;
- calendar & agenda / note / task / writing / calculator / habit tracker / cloud storage & file sync -> Productivity;
- app-management, connectivity, DNS/hosts, encryption, transfer, firewall, keyboard, network-analysis and similar utility tags -> Tools.

Unknown and deliberately ambiguous tags are ignored rather than guessed. Free-form descriptions are not classified.

If several mapped provider categories exist, an app-owned priority list selects the same category regardless of provider category ordering. This keeps classification deterministic.

## Cache

Provider metadata is derived/rebuildable state and is stored separately from user-owned organizer state in:

```text
supported-metadata-cache.json
```

The cache is a small versioned app-owned JSON document containing:

- provider identity;
- last successful refresh time;
- package IDs checked by that refresh, including provider misses;
- useful metadata for matching packages.

Writes use a temporary file and replacement move. Malformed or unsupported cache data is ignored safely.

The cache is not part of the organizer backup/export format. Backup/import continues to preserve only user-owned organization state and does not depend on provider cache availability.

## Refresh policy

The refresh interval is 14 days for this first provider. A refresh is also required when the currently installed package set includes a package that was not covered by the cached lookup.

Behavior is:

```text
scan installed apps locally
    |
publish local classification immediately
    |
load derived metadata cache on IO scope
    |
apply useful cached enrichment
    |
if stale/incomplete: request F-Droid index best-effort
    |
on success: deterministically replace derived cache/state
on failure: retain stale useful cache and local classification
```

There is no WorkManager job, permanent service or timer. Refresh is opportunistic during foreground repository refresh and runs on the process IO scope.

The provider response is capped at 64 MiB. If the documented index grows beyond that bound, enrichment fails closed and the local classifier continues to work; the limit must then be deliberately reviewed rather than silently accepting unbounded memory/network cost.

## Networking choice

The implementation uses Android/JDK `HttpURLConnection` plus the already-direct `kotlinx-serialization-json` dependency.

No Retrofit, OkHttp, Ktor or other networking framework is added. The first provider needs a single bounded HTTPS GET, so a framework would add dependency and verification surface without solving a current product problem.

No normal JVM test depends on live Internet access. Provider tests inject an app-owned document source and synthetic index fixtures.

## Permission audit

This PR adds:

```text
android.permission.INTERNET
```

It is required solely for the supported F-Droid provider implementation.

`android.permission.QUERY_ALL_PACKAGES` remains forbidden. The CI permission audit is changed from “no Internet at all” to an allow-list check that requires `INTERNET` and fails on any other declared Android permission. This is a narrow audit update, not a broad relaxation.

No broad storage permission is added.

## Privacy

A refresh makes one HTTPS request for the public F-Droid index.

Because the whole index is fetched, the app does **not** transmit its installed package list as query parameters, request bodies or per-package lookup requests. F-Droid/the network can observe an ordinary request for the public index and associated connection metadata such as the client IP address, but not Organizer's favourites, hidden state, custom categories, manual category assignments, search text or category names.

Locally, the derived cache stores only package IDs that were checked, supported provider identity, useful provider category strings and refresh/fetch timestamps.

There is no telemetry, analytics SDK, advertising SDK, account, upload endpoint or cloud synchronization introduced by metadata enrichment.

## Failure and offline behavior

Network failure, timeout, non-200 HTTP response, over-size response, malformed provider JSON and provider misses do not block installed-app scanning and do not make apps disappear.

When usable stale cache exists, it remains available after provider failure. Without usable cached metadata, classification simply follows the existing local chain and ends in `Unsorted` where no local evidence exists.

User overrides and bundled deterministic rules are never weakened by provider availability or cache contents. Android-declared category also remains above metadata in the first rollout.

## Tests

The Agent-100 unit coverage includes:

- F-Droid index adapter parsing;
- requested package lookup and explicit misses;
- malformed provider data;
- provider/network failure conversion;
- cache round trip and malformed-cache handling;
- deterministic cache encoding;
- fresh-cache no-refresh behavior;
- stale-cache refresh behavior;
- stale-cache retention on provider failure;
- refresh when a newly installed package was not checked by the fresh cache;
- deterministic provider-category mapping;
- unknown/ambiguous tag fallback;
- user override > metadata;
- bundled rule > metadata;
- Android-declared category > metadata;
- metadata > `Unsorted`;
- custom override identity preservation.

The permanent CI lane remains responsible for debug assembly, AndroidTest APK compilation, JVM tests, Android Lint, ktlint, dependency health, strict dependency verification, warning failure, configuration-cache reuse and the permission audit.

## Agent 102 combined acceptance

Agent 100 does not claim combined physical acceptance. Agent 102 must verify the merged PR-23 implementation together with the PR-22 two-destination navigation on the Samsung target device:

1. first shelf/useful local classification is not delayed by provider I/O;
2. an F-Droid-covered formerly-`Unsorted` package can improve after enrichment;
3. the classification explanation reports supported metadata when it decides category;
4. cached enrichment remains after restart/offline use;
5. airplane mode/provider failure leaves the local shelf and launch flow usable;
6. existing user overrides, bundled rules and Android-resolved categories do not change;
7. no custom-category state/order/identity is mutated;
8. interaction with the merged primary navigation remains correct: shortcuts land on `Organizer`, metadata explanation remains contextual, and metadata does not create a third destination;
9. practical network/memory cost of the current F-Droid bulk index is acceptable on the Samsung target;
10. `INTERNET` is intentional and `QUERY_ALL_PACKAGES`, broad storage, telemetry, accounts/cloud sync and background services remain absent.
