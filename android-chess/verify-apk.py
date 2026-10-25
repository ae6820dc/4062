"""Verify the actual APK, unchanged source bytes, catalog and release immutability."""
from pathlib import Path
import hashlib, json, subprocess, sys, zipfile
repo = Path(__file__).resolve().parent.parent
apk = Path(sys.argv[1])
with zipfile.ZipFile(apk) as z:
    catalog = json.loads(z.read('assets/catalog.json'))
    checksums = json.loads(z.read('assets/source-checksums.json'))
    expected = {str(p.relative_to(repo)) for p in (repo/'chess/3').rglob('sample*.html')}
    assert {p['path'] for p in catalog} == expected
    for name, sha in checksums.items():
        original = (repo/name).read_bytes()
        assert z.read('assets/site/'+name) == original
        assert hashlib.sha256(original).hexdigest() == sha
    assert len({p['programId'] for p in catalog}) == len(catalog)
    assert len({p['storageKey'] for p in catalog}) == len(catalog)
for filename in subprocess.check_output(['git','ls-files','apk/*.apk'],cwd=repo,text=True).splitlines():
    # Compare working bytes with the published Git object.
    assert (repo/filename).read_bytes() == subprocess.check_output(['git','show','HEAD:'+filename],cwd=repo)
print(f'PASS: {len(catalog)} samples; all bundled sources byte-identical; previous APKs unchanged')
