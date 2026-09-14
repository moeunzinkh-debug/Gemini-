"""Bump the bundle version and open a release notes file for the next tag.

Usage: python3 scripts/prepare-release.py 0.2.0
Then edit docs/releases/v0.2.0.md, commit, tag v0.2.0 and push the tag.
"""
from pathlib import Path
from datetime import datetime, timezone
import json
import re
import sys

REPO = 'https://github.com/moeunzinkh-debug/Gemini-'

if len(sys.argv) != 2 or not re.fullmatch(r'\d+\.\d+\.\d+', sys.argv[1]):
    raise SystemExit('Usage: prepare-release.py MAJOR.MINOR.PATCH')
version = sys.argv[1]
name = f'gemini-standalone-patches-{version}.mpp'

root = Path(__file__).resolve().parents[1]
metadata_path = root / 'patches-bundle.json'
metadata = json.loads(metadata_path.read_text())
metadata['version'] = version
metadata['created_at'] = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace('+00:00', '')
metadata['download_url'] = f'{REPO}/releases/download/v{version}/{name}'
metadata['page_url'] = f'{REPO}/releases/tag/v{version}'
metadata_path.write_text(json.dumps(metadata, indent=2, sort_keys=True) + '\n')

notes = root / 'docs' / 'releases' / f'v{version}.md'
if notes.exists():
    print(f'Release notes already exist: {notes.relative_to(root)}')
else:
    notes.parent.mkdir(parents=True, exist_ok=True)
    notes.write_text(
        f'# {version}\n\n'
        '## ការផ្លាស់ប្តូរ / Changes\n\n'
        '- \n\n'
        '## ការផ្ទៀងផ្ទាត់ / Verified\n\n'
        '- \n\n'
        '## មិនទាន់បានផ្ទៀងផ្ទាត់ / Not verified\n\n'
        '- \n'
    )
    print(f'Created {notes.relative_to(root)} - fill it in before tagging.')

# Fail here rather than in CI if the metadata no longer matches what package-mpp.py expects.
from datetime import datetime as _dt  # noqa: E402
_dt.fromisoformat(metadata['created_at'])
print(f'Prepared v{version}: {metadata["download_url"]}')
