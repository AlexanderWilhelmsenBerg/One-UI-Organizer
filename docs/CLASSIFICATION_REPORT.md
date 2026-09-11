# Local Classification Report

The classification report is an explicit owner diagnostic for tuning deterministic rules from real launcher evidence. It is not telemetry and is not authoritative organizer state.

## Generate on the Samsung owner device

1. Install/run a debug APK containing the Agent 60 classification foundation.
2. Open One UI Organizer and allow the normal launcher scan to finish.
3. Scroll to the bottom of the shelf and tap **Share classification report**.
4. Choose a private/local destination in the Android share chooser, or share directly into the private agent conversation used for analysis.
5. Supply that report privately for taxonomy/rule review. Do **not** commit it, attach it to a GitHub issue/PR, or add it as a test fixture.

The action is disabled until the organizer has current launcher results. Generating the report performs no additional package scan, does not write organizer state, and adds no network permission or background collection.

## Format

The current plain-text format starts with `formatVersion=1` and contains deterministic aggregate sections followed by launcher-target rows.

Aggregate sections:

- organizer category counts, emitted in `AppCategory.entries` order and including zero counts;
- classification source counts, emitted in `ClassificationSource.entries` order and including zero counts.

Each target row contains tab-separated:

```text
label
packageName
className
platformCategory
organizerCategory
classificationSource
```

Rows are ordered by package name, exact component class name, then label. Tabs and line breaks in text fields are replaced with spaces so one launcher target always occupies one row.

## Privacy and repository policy

A report can disclose the owner's installed launcher inventory and therefore remains private diagnostic material. Repository history should contain only generalized decisions, aggregate before/after counts where useful, reusable deterministic selectors/rules, regression tests and sanitized documentation.

The app itself has no report upload endpoint. Android's standard share chooser is invoked only after the user taps the diagnostic action; the destination is selected by the user.
