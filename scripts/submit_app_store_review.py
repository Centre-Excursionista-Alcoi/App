import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request

import jwt

API_BASE = "https://api.appstoreconnect.apple.com/v1"

# Apple's JWTs for this API are capped at 20 minutes -- regenerated before every request instead of once
# upfront, since waiting for build processing can take much longer than that.
TOKEN_LIFETIME_SECONDS = 19 * 60

# How long to keep polling for the build to finish processing before giving up.
BUILD_PROCESSING_TIMEOUT_SECONDS = 30 * 60
BUILD_PROCESSING_POLL_INTERVAL_SECONDS = 60


def make_token(private_key: str, key_id: str, issuer_id: str) -> str:
    now = int(time.time())
    payload = {
        "iss": issuer_id,
        "iat": now,
        "exp": now + TOKEN_LIFETIME_SECONDS,
        "aud": "appstoreconnect-v1",
    }
    return jwt.encode(payload, private_key, algorithm="ES256", headers={"kid": key_id, "typ": "JWT"})


def api_request(method: str, path: str, private_key: str, key_id: str, issuer_id: str, body: dict | None = None) -> dict:
    """Makes a single App Store Connect API request, signing a fresh token for it."""

    token = make_token(private_key, key_id, issuer_id)
    url = path if path.startswith("http") else f"{API_BASE}{path}"
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", f"Bearer {token}")
    if data is not None:
        request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request) as response:
            raw = response.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        print(f"App Store Connect API {method} {path} failed: {e.code}")
        print(e.read().decode())
        raise


def wait_for_build(app_id: str, bundle_version: str, bundle_short_version: str, private_key: str, key_id: str, issuer_id: str) -> str:
    """Polls until the just-uploaded build finishes processing, returning its id.

    A build only becomes attachable to an App Store version once Apple finishes processing it (virus scan,
    binary validation, ...) -- this routinely takes several minutes after `altool --upload-package` returns.
    """

    deadline = time.time() + BUILD_PROCESSING_TIMEOUT_SECONDS
    path = (
        f"/builds?filter[app]={app_id}&filter[version]={bundle_version}"
        f"&filter[preReleaseVersion.version]={bundle_short_version}"
    )
    print(f"Waiting for build {bundle_short_version} ({bundle_version}) to finish processing...")
    while True:
        result = api_request("GET", path, private_key, key_id, issuer_id)
        builds = result.get("data", [])
        if builds:
            build = builds[0]
            state = build["attributes"]["processingState"]
            print(f"  processingState={state}")
            if state == "VALID":
                return build["id"]
            if state in ("FAILED", "INVALID"):
                print(f"Build processing ended in state {state} -- cannot proceed.")
                sys.exit(1)
        if time.time() >= deadline:
            print(f"Timed out after {BUILD_PROCESSING_TIMEOUT_SECONDS}s waiting for the build to finish processing.")
            sys.exit(1)
        time.sleep(BUILD_PROCESSING_POLL_INTERVAL_SECONDS)


def find_or_create_version(app_id: str, version_string: str, private_key: str, key_id: str, issuer_id: str) -> str:
    """Returns the id of the app store version for this release, creating it if it doesn't exist yet.

    Reuses an existing version in PREPARE_FOR_SUBMISSION-like states (e.g. a previous run of this same
    workflow that got interrupted after creating the version but before submitting) rather than erroring on
    a duplicate versionString.
    """

    existing = api_request(
        "GET",
        f"/apps/{app_id}/appStoreVersions?filter[versionString]={version_string}&filter[platform]=IOS",
        private_key, key_id, issuer_id,
    )
    versions = existing.get("data", [])
    if versions:
        version_id = versions[0]["id"]
        print(f"Reusing existing App Store version {version_id} ({version_string}).")
        return version_id

    created = api_request(
        "POST", "/appStoreVersions", private_key, key_id, issuer_id,
        body={
            "data": {
                "type": "appStoreVersions",
                "attributes": {
                    "platform": "IOS",
                    "versionString": version_string,
                    # Releases the instant Apple approves it -- no separate manual "Release This Version" step.
                    "releaseType": "AFTER_APPROVAL",
                },
                "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
            }
        },
    )
    version_id = created["data"]["id"]
    print(f"Created App Store version {version_id} ({version_string}).")
    return version_id


def attach_build(version_id: str, build_id: str, private_key: str, key_id: str, issuer_id: str) -> None:
    api_request(
        "PATCH", f"/appStoreVersions/{version_id}", private_key, key_id, issuer_id,
        body={
            "data": {
                "type": "appStoreVersions",
                "id": version_id,
                "relationships": {"build": {"data": {"type": "builds", "id": build_id}}},
            }
        },
    )
    print(f"Attached build {build_id} to version {version_id}.")


def set_whats_new(version_id: str, locale: str, whats_new: str, private_key: str, key_id: str, issuer_id: str) -> None:
    """Sets the "What's New in This Version" text for the given locale.

    Updates the localization if one already exists for this version+locale (the common case: the app already
    has a `locale` listing from a previous release), otherwise creates one. A fresh appStoreVersions has no
    localizations of its own yet -- they aren't automatically carried over from the previous version.
    """

    existing = api_request(
        "GET",
        f"/appStoreVersions/{version_id}/appStoreVersionLocalizations?filter[locale]={locale}",
        private_key, key_id, issuer_id,
    )
    localizations = existing.get("data", [])
    if localizations:
        localization_id = localizations[0]["id"]
        api_request(
            "PATCH", f"/appStoreVersionLocalizations/{localization_id}", private_key, key_id, issuer_id,
            body={
                "data": {
                    "type": "appStoreVersionLocalizations",
                    "id": localization_id,
                    "attributes": {"whatsNew": whats_new},
                }
            },
        )
        print(f"Updated '{locale}' localization {localization_id} with What's New text.")
    else:
        api_request(
            "POST", "/appStoreVersionLocalizations", private_key, key_id, issuer_id,
            body={
                "data": {
                    "type": "appStoreVersionLocalizations",
                    "attributes": {"locale": locale, "whatsNew": whats_new},
                    "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}}},
                }
            },
        )
        print(f"Created '{locale}' localization with What's New text.")


def submit_for_review(version_id: str, private_key: str, key_id: str, issuer_id: str) -> None:
    api_request(
        "POST", "/appStoreVersionSubmissions", private_key, key_id, issuer_id,
        body={
            "data": {
                "type": "appStoreVersionSubmissions",
                "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}}},
            }
        },
    )
    print(f"Submitted version {version_id} for review.")


def main():
    parser = argparse.ArgumentParser(description="Create an App Store version, attach the just-uploaded build, and submit it for review.")
    parser.add_argument("--api-key-id", required=True)
    parser.add_argument("--issuer-id", required=True)
    parser.add_argument("--app-id", required=True, help="The app's numeric App Store Connect id.")
    parser.add_argument("--bundle-version", required=True, help="CFBundleVersion of the uploaded build.")
    parser.add_argument("--bundle-short-version", required=True, help="CFBundleShortVersionString -- also the App Store version string.")
    parser.add_argument("--whats-new", required=True)
    parser.add_argument("--locale", default="ca")
    args = parser.parse_args()

    private_key_path = os.path.expanduser(f"~/.private_keys/AuthKey_{args.api_key_id}.p8")
    if not os.path.exists(private_key_path):
        print(f"Private key not found at {private_key_path}.")
        sys.exit(1)
    with open(private_key_path) as f:
        private_key = f.read()

    build_id = wait_for_build(args.app_id, args.bundle_version, args.bundle_short_version, private_key, args.api_key_id, args.issuer_id)
    version_id = find_or_create_version(args.app_id, args.bundle_short_version, private_key, args.api_key_id, args.issuer_id)
    attach_build(version_id, build_id, private_key, args.api_key_id, args.issuer_id)
    set_whats_new(version_id, args.locale, args.whats_new, private_key, args.api_key_id, args.issuer_id)
    submit_for_review(version_id, private_key, args.api_key_id, args.issuer_id)


if __name__ == "__main__":
    main()
