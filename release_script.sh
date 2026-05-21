#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# TripTracker Release Script
# Updates version in all config files, commits, tags, and pushes.
# 
# Usage:
#   ./release.sh 1.0.50
#   ./release.sh 1.0.50 "Fix GPS cold start speed"
# ═══════════════════════════════════════════════════════════════

set -e

VERSION=$1
MESSAGE=${2:-"Release v$VERSION"}

if [ -z "$VERSION" ]; then
    echo "❌ Usage: ./release.sh <version> [message]"
    echo "   Example: ./release.sh 1.0.50 \"Fix GPS cold start speed\""
    exit 1
fi

echo "🚀 Releasing TripTracker v$VERSION"
echo "   Message: $MESSAGE"
echo ""

# ── 1. Root package.json ──
ROOT_PKG="package.json"
if [ -f "$ROOT_PKG" ]; then
    sed -i '' "s/\"version\": \"[^\"]*\"/\"version\": \"$VERSION\"/" "$ROOT_PKG"
    echo "✅ $ROOT_PKG → $VERSION"
fi

# ── 2. Root package-lock.json ──
ROOT_LOCK="package-lock.json"
if [ -f "$ROOT_LOCK" ]; then
    # Update only the top-level "version" (first occurrence)
    sed -i '' "0,/\"version\": \"[^\"]*\"/s/\"version\": \"[^\"]*\"/\"version\": \"$VERSION\"/" "$ROOT_LOCK"
    echo "✅ $ROOT_LOCK → $VERSION"
fi

# ── 3. Capacitor plugin package.json ──
CAP_PKG="triptracking-library/capacitor_plugin/package.json"
if [ -f "$CAP_PKG" ]; then
    sed -i '' "s/\"version\": \"[^\"]*\"/\"version\": \"$VERSION\"/" "$CAP_PKG"
    echo "✅ $CAP_PKG → $VERSION"
fi

# ── 4. Capacitor plugin package-lock.json ──
CAP_LOCK="triptracking-library/capacitor_plugin/package-lock.json"
if [ -f "$CAP_LOCK" ]; then
    sed -i '' "0,/\"version\": \"[^\"]*\"/s/\"version\": \"[^\"]*\"/\"version\": \"$VERSION\"/" "$CAP_LOCK"
    echo "✅ $CAP_LOCK → $VERSION"
fi

# ── 5. CapacitorTripTracker.podspec (root) ──
ROOT_PODSPEC="CapacitorTripTracker.podspec"
if [ -f "$ROOT_PODSPEC" ]; then
    sed -i '' "s/s\.version\s*=\s*'[^']*'/s.version          = '$VERSION'/" "$ROOT_PODSPEC"
    echo "✅ $ROOT_PODSPEC → $VERSION"
fi

# ── 6. CapacitorTripTracker.podspec (capacitor_plugin) ──
CAP_PODSPEC="triptracking-library/capacitor_plugin/CapacitorTripTracker.podspec"
if [ -f "$CAP_PODSPEC" ]; then
    sed -i '' "s/s\.version\s*=\s*'[^']*'/s.version          = '$VERSION'/" "$CAP_PODSPEC"
    echo "✅ $CAP_PODSPEC → $VERSION"
fi

# ── 7. triptracking.podspec (root) ──
ROOT_TT_PODSPEC="triptracking.podspec"
if [ -f "$ROOT_TT_PODSPEC" ]; then
    sed -i '' "s/s\.version\s*=\s*'[^']*'/s.version          = '$VERSION'/" "$ROOT_TT_PODSPEC"
    echo "✅ $ROOT_TT_PODSPEC → $VERSION"
fi

# ── 8. triptracking.podspec (ios) ──
IOS_PODSPEC="triptracking-library/ios/triptracking.podspec"
if [ -f "$IOS_PODSPEC" ]; then
    sed -i '' "s/s\.version\s*=\s*'[^']*'/s.version          = '$VERSION'/" "$IOS_PODSPEC"
    echo "✅ $IOS_PODSPEC → $VERSION"
fi

echo ""
echo "📝 Files updated. Committing..."

# ── Git: commit + tag + push ──
git add -A
git commit -m "v$VERSION — $MESSAGE"
git push origin main

echo ""
echo "🏷️  Creating tag $VERSION..."
git tag "$VERSION"
git push origin "$VERSION"

echo ""
echo "═══════════════════════════════════════════════════"
echo "✅ TripTracker v$VERSION released!"
echo ""
echo "GitHub Actions will build Android AAR automatically."
echo "Check: https://github.com/hieunguyentt/TripTracker/actions"
echo ""
echo "To update Ionic project:"
echo "  npm install \"github:hieunguyentt/TripTracker#$VERSION\""
echo "  npx cap sync"
echo "═══════════════════════════════════════════════════"
