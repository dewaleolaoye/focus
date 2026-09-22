#!/usr/bin/env python3
"""Render the same policy shipped in the app and verify owner-supplied release prerequisites."""
import argparse
import html
import os
from pathlib import Path
import re
import sys
import urllib.request
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]

def metadata():
    path = ROOT / 'release.properties'
    result = {}
    if path.exists():
        for line in path.read_text().splitlines():
            if line.strip() and not line.lstrip().startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                result[key.strip()] = value.strip()
    return result

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--draft', action='store_true', help='Render a visibly marked draft without asserting submission readiness.')
    args = parser.parse_args()
    config = metadata()
    errors = []
    for key in ('developerName', 'contactEmail', 'privacyPolicyUrl'):
        if not config.get(key): errors.append(f'Missing {key} in release.properties')
    email = config.get('contactEmail', '')
    if email and not re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+', email): errors.append('contactEmail is not a valid public email address')
    url = config.get('privacyPolicyUrl', '')
    if url and (urlparse(url).scheme != 'https' or not urlparse(url).hostname): errors.append('privacyPolicyUrl must be an HTTPS URL')
    if errors and not args.draft:
        print('NOT READY FOR PLAY SUBMISSION:', file=sys.stderr)
        for error in errors: print('- ' + error, file=sys.stderr)
        return 1
    text = (ROOT / 'app/src/main/assets/privacy-policy.txt').read_text()
    for token, key in [('DEVELOPER_NAME', 'developerName'), ('CONTACT_EMAIL', 'contactEmail'), ('POLICY_URL', 'privacyPolicyUrl')]:
        text = text.replace('{{' + token + '}}', config.get(key) or f'[PUBLISHER MUST SUPPLY {key}]')
    paragraphs = text.split('\n\n')
    body = '\n'.join('<p>' + html.escape(p).replace('\n', '<br>') + '</p>' for p in paragraphs)
    if args.draft:
        body = '<aside><strong>DRAFT — NOT FOR PUBLICATION until publisher details and release checks are complete.</strong></aside>' + body
    target = ROOT / 'docs/play/privacy-policy.html'
    target.write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Focus Privacy Policy</title><style>body{font:17px/1.6 system-ui;max-width:800px;margin:auto;padding:32px;color:#16382d}aside{padding:16px;background:#fff2bc}p{white-space:normal;overflow-wrap:anywhere}</style><main>' + body + '</main></html>')
    print(f'Privacy policy rendered: {target.relative_to(ROOT)}')
    if args.draft:
        for error in errors: print('Pending: ' + error)
        return 0
    for key in ('FOCUS_KEYSTORE_PATH', 'FOCUS_KEYSTORE_PASSWORD', 'FOCUS_KEY_ALIAS', 'FOCUS_KEY_PASSWORD'):
        if not os.environ.get(key): errors.append(f'Missing environment variable {key}')
    keypath = os.environ.get('FOCUS_KEYSTORE_PATH')
    if keypath and not (ROOT / keypath).is_file(): errors.append('Upload keystore file not found')
    if not errors:
        try:
            with urllib.request.urlopen(url, timeout=20) as response:
                published = response.read(2_000_000).decode('utf-8')
                if urlparse(response.url).scheme != 'https': errors.append('Public policy redirects away from HTTPS')
                # Require the actual generated policy, not a login page or placeholder URL.
                if published.strip() != target.read_text().strip(): errors.append('Hosted privacy policy does not match docs/play/privacy-policy.html')
        except Exception as exc:
            errors.append('Public privacy policy is not accessible: ' + type(exc).__name__)
    if errors:
        print('\nNOT READY FOR PLAY SUBMISSION:', file=sys.stderr)
        for error in errors: print('- ' + error, file=sys.stderr)
        return 1
    print('Local prerequisites passed. Play Console declarations, AdMob message configuration and Google review remain separate.')
    return 0

if __name__ == '__main__':
    sys.exit(main())
