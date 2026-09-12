# Local Backup Format v1

This document defines the portable local backup contract introduced by Agent 90.

The backup contract is deliberately separate from the DataStore persistence contract. `OrganizerBackupFormatVersion` starts at **1** and evolves independently of `OrganizerState.schemaVersion`, which is currently **2**. Changing DataStore storage or migrating organizer state must not silently change the portable file format.

## Scope

A backup contains only user-owned organizer preferences:

- app-to-category overrides;
- favourite package IDs;
- hidden package IDs;
- custom-category stable IDs and display names;
- persisted category order.

A backup does **not** contain:

- installed-app labels;
- app icons;
- launcher components or cached scanner output;
- Android `PackageManager` data;
- diagnostic launcher inventories or classification reports;
- Android framework, DataStore or serialization implementation objects.

Package IDs that are not currently installed remain legitimate user-owned state. Export preserves them and import does not consult the current installed-app scan before accepting them.

## Identity

Portable identity is copied exactly from the accepted category-management contract.

- Built-in categories use their explicit `builtin:*` `CategoryId` values.
- Custom categories use their existing `custom:<opaque-id>` values.
- Custom IDs are never regenerated during import.
- Renaming a custom category changes only its display name; backup/restore keeps the same ID.
- App identity is the existing package-level `AppId` package name.

## JSON v1

The file is UTF-8 JSON with MIME type `application/json`. The suggested filename is `one-ui-organizer-backup.json`.

All v1 fields shown below are required:

```json
{
  "formatVersion": 1,
  "categoryOverrides": [
    {
      "appId": "com.example.reader",
      "categoryId": "builtin:reading"
    },
    {
      "appId": "com.example.uninstalled",
      "categoryId": "custom:4caeddb0-2b39-4eb5-bb77-6b67f5018965"
    }
  ],
  "favouriteAppIds": [
    "com.example.reader"
  ],
  "hiddenAppIds": [
    "com.example.uninstalled"
  ],
  "customCategories": [
    {
      "categoryId": "custom:4caeddb0-2b39-4eb5-bb77-6b67f5018965",
      "name": "Offline"
    }
  ],
  "categoryOrder": [
    "custom:4caeddb0-2b39-4eb5-bb77-6b67f5018965",
    "builtin:communication"
  ]
}
```

The example category order is abbreviated for readability. A real v1 backup contains an exact duplicate-free permutation of every current built-in category plus every custom category defined by the backup.

## Deterministic export

Export is deterministic for equivalent organizer state:

- category overrides are ordered by package name;
- favourites are ordered by package name;
- hidden package IDs are ordered by package name;
- custom-category definition order is preserved;
- the normalized persisted category order is preserved;
- JSON field order is fixed by the v1 encoder.

The deterministic representation makes backups easier to review and gives tests a stable portable contract. It is not a promise that later backup-format versions will use identical textual layout.

## Parsing and forward compatibility

The v1 reader requires every documented v1 field and the documented field type. Malformed JSON, missing required fields or incompatible field types are fatal.

Unknown fields are ignored when reading a v1 document. This permits additive metadata that does not change v1 meaning. A different `formatVersion` is **not** treated as v1 with fields discarded: unsupported versions are rejected explicitly before any organizer state is changed.

A future format version must receive an explicit decoder/migration decision. It must not reuse the DataStore schema number as the backup-format number.

## Complete validation before mutation

Selecting a file does not import it. The document is read, parsed and fully validated into a prepared app-owned import before confirmation is shown.

Import rejects the entire document if any fatal invariant fails, including:

- unsupported backup format version;
- malformed JSON or required-field/type failure;
- duplicate custom-category IDs;
- a custom ID outside the `custom:` namespace or with an empty/invalid opaque portion;
- a custom name that violates the same `CategoryNamePolicy` used by category management, including blank, overlong or duplicate names;
- blank/invalid package identity;
- duplicate override, favourite or hidden package identity;
- an override that references neither a built-in category nor a custom category defined by the backup;
- duplicate, unknown or missing IDs in category order.

Validation does not silently normalize a damaged category order or discard invalid records. The persistence store's tolerant read normalization is intentionally **not** the portable import policy.

Parser/storage exceptions are converted to app-owned backup errors. Presentation receives sanitized error categories and never a raw parsing or DataStore exception.

## Import semantics and atomicity

After full validation, the UI shows a sanitized summary containing only counts for:

- custom categories;
- category overrides;
- favourites;
- hidden apps.

The user must explicitly confirm replacement. Confirmation performs one `OrganizerStateStore.update` operation using the prepared state. DataStore's `updateData` transaction is therefore the atomic persistence boundary.

A successful import replaces all organizer-owned organization state represented by the format. It does not modify the installed-app scan or Android system state.

If validation fails, no persistence call is made. If persistence fails, the store does not commit a partial imported state and the existing organizer state remains current.

After success, consumers observe the normal shared `OrganizerStateStore`/repository state. The backup feature does not maintain a second persistent imported-state cache.

## Storage Access Framework

Android file access uses the Storage Access Framework only:

- export uses the system create-document contract equivalent to `ACTION_CREATE_DOCUMENT`;
- import uses the system open-document contract equivalent to `ACTION_OPEN_DOCUMENT`;
- no broad storage permission is requested;
- no custom file browser is implemented.

Android `Uri`, `ContentResolver` and activity-result contract types remain in the platform/presentation adapter boundary. The core backup repository receives only app-owned document content/models and `OrganizerStateStore`.

Agent 90 exposes the controller, SAF adapter and Compose surface without modifying `MainActivity` or the process composition root. Agent 92 owns the final cross-feature wiring, including giving `DefaultOrganizerBackupRepository` the same `OrganizerStateStore` instance as the organizer/category repositories and connecting app-owned document requests to activity-result launchers.

## Privacy and security

A backup contains personal organization preferences and package identifiers. Treat it as user data.

- no network transmission;
- no cloud backend or automatic synchronization;
- no telemetry or analytics;
- no automatic upload;
- no logging of full backup contents;
- no raw personal backup fixture committed to the repository;
- no `INTERNET` permission;
- no `QUERY_ALL_PACKAGES` permission.

Only the user-selected SAF destination/source is accessed.
