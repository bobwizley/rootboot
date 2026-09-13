#!/usr/bin/env bash

set -euo pipefail

tag="${GITEA_REF_NAME:?GITEA_REF_NAME is required}"
api_url="${GITEA_API_URL:?GITEA_API_URL is required}"
repository="${GITEA_REPOSITORY:?GITEA_REPOSITORY is required}"
token="${GITEA_TOKEN:?GITEA_TOKEN is required}"

if [[ ! "$tag" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "Release tag must match vMAJOR.MINOR.PATCH: $tag" >&2
    exit 1
fi

version="${tag#v}"
tag_commit="$(git rev-parse "${tag}^{commit}")"

if ! git merge-base --is-ancestor "$tag_commit" refs/remotes/origin/master; then
    echo "Release tag $tag does not point to a commit in master history" >&2
    exit 1
fi

auth_header="Authorization: token $token"
json_header="Content-Type: application/json"
releases_url="$api_url/repos/$repository/releases"

lookup="$(
    curl \
        --silent \
        --show-error \
        --write-out '\n%{http_code}' \
        --header "$auth_header" \
        "$releases_url/tags/$tag"
)"
lookup_status="${lookup##*$'\n'}"
release_json="${lookup%$'\n'*}"

case "$lookup_status" in
    200) ;;
    404) release_json="" ;;
    *)
        echo "Unexpected response $lookup_status while looking up release $tag" >&2
        echo "$release_json" >&2
        exit 1
        ;;
esac

asset_name="rootboot-$version.jar"
asset_path="build/libs/$asset_name"

if [[ ! -f "$asset_path" ]]; then
    echo "Expected release asset is missing: $asset_path" >&2
    exit 1
fi

if [[ -z "$release_json" ]]; then
    previous_tag="$(git describe --tags --abbrev=0 "${tag}^")"
    notes="$(git log --format='- %s' "$previous_tag..$tag")"

    release_json="$(
        jq \
            --null-input \
            --arg tag "$tag" \
            --arg body "$notes" \
            '{tag_name: $tag, name: $tag, body: $body, draft: false, prerelease: false}' |
            curl \
                --fail-with-body \
                --silent \
                --show-error \
                --request POST \
                --header "$auth_header" \
                --header "$json_header" \
                --data @- \
                "$releases_url"
    )"

    echo "Created release $tag"
else
    if [[ "$(jq -r '.tag_name' <<<"$release_json")" != "$tag" ]]; then
        echo "Release tag returned by Gitea does not match $tag" >&2
        exit 1
    fi

    release_json="$(
        jq --null-input '{draft: false, prerelease: false}' |
            curl \
                --fail-with-body \
                --silent \
                --show-error \
                --request PATCH \
                --header "$auth_header" \
                --header "$json_header" \
                --data @- \
                "$releases_url/$(jq -er '.id' <<<"$release_json")"
    )"
fi

assets_url="$releases_url/$(jq -er '.id' <<<"$release_json")/assets"

assets_json="$(
    curl \
        --fail-with-body \
        --silent \
        --show-error \
        --header "$auth_header" \
        "$assets_url"
)"

if jq -e --arg name "$asset_name" '.[] | select(.name == $name)' <<<"$assets_json" >/dev/null; then
    echo "Release asset $asset_name already exists; nothing to publish"
    exit 0
fi

curl \
    --fail-with-body \
    --silent \
    --show-error \
    --request POST \
    --header "$auth_header" \
    --form "attachment=@$asset_path;type=application/java-archive" \
    "$assets_url?name=$asset_name" \
    >/dev/null

echo "Published $asset_name to release $tag"
