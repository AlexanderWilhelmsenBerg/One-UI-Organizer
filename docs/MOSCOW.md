# MoSCoW Scope Analysis

This document separates the first useful release from attractive follow-on ideas. The goal is to keep One UI Organizer tiny enough to finish while preserving a clear path to richer organization later.

## Must have — v0.1 cannot ship without these

### Companion behaviour

- One UI Home remains the default launcher.
- Organizer opens as a separate companion activity/shelf rather than replacing the launcher.
- The user can dismiss Organizer and return directly to the existing home experience.

### Supported app discovery

- Discover ordinary launchable apps through supported Android APIs.
- Avoid `QUERY_ALL_PACKAGES` in v0.1.
- Correctly model package/component identity well enough to handle launcher aliases and multiple launch activities deterministically.
- Refresh the discovered set when Organizer is opened/resumed so installs and removals appear without a persistent service.

### Automatic categorization

- Deterministic categorization pipeline:
  1. user override;
  2. known-app rule;
  3. Android-declared category mapping;
  4. `Unsorted`.
- `Unsorted` must remain a fully usable category, not an error state.
- A bundled starter ruleset for common app classes/categories.

### Manual correction

- User can change an app's primary category.
- User correction persists locally.
- User correction remains authoritative if automatic rules change later.

### Basic app shelf

- Category browsing.
- App-name search.
- Category-name search.
- Tap to launch.
- Favourite/unfavourite.
- Hide and later restore hidden apps.
- Light/dark readability and basic accessibility semantics.

### Privacy / minimalism

- No account.
- No analytics or ads.
- No cloud service.
- No `INTERNET` permission.
- No background service for ordinary v0.1 behaviour.

### Engineering quality

- Clean build from checkout.
- Testable package-scanner boundary.
- Pure Kotlin category engine with unit coverage.
- Persistence tests for user overrides/favourites/hidden state.
- Critical Compose/UI tests.
- Physical Samsung device acceptance on modern One UI / Android 16.

## Should have — high-value follow-up, but v0.1 remains useful without it

### Custom categories

- Create, rename, and delete user categories.
- Reorder categories.
- Choose a category icon from a bundled icon set.

### Better category management

- Dedicated hidden-app management screen.
- Dedicated `Unsorted` triage flow.
- Bulk move several apps to a category.
- Rule explanation such as "Placed here by Android category" or "Your override".

### Shortcuts

- Android dynamic shortcuts for favourite categories.
- Ability to request a pinned home-screen shortcut for a selected category.

### Backup / portability

- Export organizer rules and user overrides to a local file.
- Import a previously exported local rules file.
- Version the export format from its first release.

### Presentation polish

- More complete One UI-inspired spacing and motion tokens.
- Smooth transition from home screen into the Organizer surface.
- Optional blur only where the platform/device reliably supports it.

### Performance hardening

- Baseline profile / macrobenchmark once the product flow is stable enough that measurements are meaningful.

## Could have — useful ideas that should not delay the core app

### Home-screen widget

- Glance widget showing favourites or selected categories.
- Configurable 4x1 / 4x2 layouts.

### Multiple tags/categories

- Allow one app to belong to more than one category while retaining one primary category for normal browsing.
- Example: Home Assistant could appear under both Smart Home and Homelab.

### Usage-based suggestions

- Optional, explicit permission-based "frequently used" or time-of-day suggestions.
- Must remain opt-in and must work locally.

### Work profile support

- Distinguish personal and work-profile launch targets.
- Category/rule state scoped correctly per profile.

### Better search

- Fuzzy matching.
- Acronym/initial matching.
- Search aliases supplied by the user.

### Rule tooling

- User-editable automatic rules.
- Import community-maintained rule packs from a local file.
- Explain why a rule matched.

### Smart sorting

- Manual category order.
- Alphabetical, recently installed, or user-defined order within categories.
- Optional local usage ranking if the user grants usage access.

### Tablet / foldable adaptation

- Material 3 adaptive layouts for large screens/foldables if real use warrants it.

## Won't have — explicitly out of scope for v0.1

These are not declarations that the project can never change. They are deliberate exclusions from the first product slice.

### Native One UI manipulation

- Rewriting Samsung's native app-drawer order.
- Creating/editing Samsung launcher folders programmatically.
- Writing Samsung launcher databases.
- Depending on undocumented Samsung launcher APIs.

### Launcher replacement

- Becoming the default Android HOME application.
- Reimplementing widgets, wallpaper handling, launcher gestures, recents, or the One UI home screen.

### Cloud / AI classification

- Server-side app classification.
- LLM classification.
- User accounts or sync backend.
- Remote analytics/telemetry.

### Broad package access for convenience

- `QUERY_ALL_PACKAGES` unless a later product requirement both genuinely needs it and satisfies applicable distribution policy.

### Always-on monitoring

- Permanent foreground/background service.
- Constant package-change monitoring solely to avoid a cheap rescan when the Organizer opens.

### Cross-platform implementation

- iOS version.
- Desktop version.
- A cross-platform framework solely for hypothetical future portability.

## Priority interpretation

When a new idea appears during implementation:

1. If it is required for a Must item to work correctly, include it.
2. If it improves quality but does not block a Must item, record it under Should or Could.
3. If it expands the product into launcher replacement, cloud services, or unsupported Samsung internals, defer it unless the project scope is explicitly re-approved.
4. Prefer finishing one polished companion workflow over shipping many half-connected surfaces.
