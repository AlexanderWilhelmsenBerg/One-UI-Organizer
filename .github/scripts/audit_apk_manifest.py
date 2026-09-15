#!/usr/bin/env python3
"""Audit the merged manifest extracted from the built release APK."""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID = "{http://schemas.android.com/apk/res/android}"
EXPECTED_PLATFORM_PERMISSIONS = {"android.permission.INTERNET"}
EXPECTED_EXPORTED = {("activity", ".MainActivity")}


def fail(message: str) -> None:
    raise SystemExit(f"APK manifest audit failed: {message}")


def main() -> None:
    if len(sys.argv) != 2:
        fail("expected path to decoded merged manifest")

    manifest_path = Path(sys.argv[1])
    root = ET.parse(manifest_path).getroot()
    package_name = root.get("package")
    if not package_name:
        fail("manifest package/application id is missing")

    receiver_permission = f"{package_name}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    expected_permissions = EXPECTED_PLATFORM_PERMISSIONS | {receiver_permission}
    permissions = {
        element.get(f"{ANDROID}name")
        for tag in ("uses-permission", "uses-permission-sdk-23")
        for element in root.findall(tag)
        if element.get(f"{ANDROID}name")
    }
    if permissions != expected_permissions:
        fail(
            "unexpected merged permission set: "
            + (", ".join(sorted(permissions)) if permissions else "no permissions")
        )

    receiver_permission_declarations = [
        element
        for element in root.findall("permission")
        if element.get(f"{ANDROID}name") == receiver_permission
    ]
    if len(receiver_permission_declarations) != 1:
        fail("AndroidX dynamic-receiver permission must have exactly one declaration")
    protection_level = receiver_permission_declarations[0].get(f"{ANDROID}protectionLevel")
    if protection_level != "signature":
        fail(
            "AndroidX dynamic-receiver permission must be signature-protected, got "
            f"{protection_level!r}"
        )

    application = root.find("application")
    if application is None:
        fail("application element is missing")

    if application.get(f"{ANDROID}allowBackup") != "false":
        fail("android:allowBackup must remain false")

    if application.get(f"{ANDROID}usesCleartextTraffic") == "true":
        fail("android:usesCleartextTraffic must not be true")

    exported: set[tuple[str, str]] = set()
    for tag in ("activity", "activity-alias", "service", "receiver", "provider"):
        for element in application.findall(tag):
            if element.get(f"{ANDROID}exported") == "true":
                name = element.get(f"{ANDROID}name")
                if not name:
                    fail(f"exported {tag} has no android:name")
                exported.add((tag, name))

    normalized_exported = {
        (kind, ".MainActivity" if name.endswith(".MainActivity") else name)
        for kind, name in exported
    }
    if normalized_exported != EXPECTED_EXPORTED:
        fail(f"unexpected exported components: {sorted(exported)!r}")

    print("APK manifest audit passed.")
    print("Platform permissions: android.permission.INTERNET")
    print(f"App-scoped signature permission: {receiver_permission}")
    print("Exported components: MainActivity only")
    print("Backup: disabled")
    print("Cleartext traffic: not explicitly enabled")


if __name__ == "__main__":
    main()
