"""Discover samples and copy the complete source folder without transformations."""
from pathlib import Path
import hashlib, json, re, shutil
from html.parser import HTMLParser
project = Path(__file__).resolve().parent
repo = project.parent
source = repo / 'chess/3'
output = project / 'app/build/generated/offline'
def natural(path):
    return [int(s) if s.isdigit() else s for s in re.split(r'(\d+)', str(path))]
samples = sorted(source.rglob('sample*.html'), key=natural)
assert samples, 'No chess samples found'
if output.exists(): shutil.rmtree(output)
(output / 'site/chess/3').mkdir(parents=True)
# Copy every file, retaining relative paths, including all future local assets.
checksums = {}
for path in sorted(source.rglob('*')):
    if not path.is_file(): continue
    relative = path.relative_to(repo)
    target = output / 'site' / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, target)
    checksums[str(relative)] = hashlib.sha256(path.read_bytes()).hexdigest()
class References(HTMLParser):
    def handle_starttag(self, tag, attrs):
        for key, value in attrs:
            if key in ('src', 'href') and value and not value.startswith(('#', 'data:', 'blob:')):
                assert not re.match(r'(https?:)?//', value), f'Unbundled dependency: {value}'
                assert (self.path.parent / value.split('?')[0].split('#')[0]).is_file(), f'Missing dependency: {value}'
catalog = []
for path in samples:
    html = path.read_text()
    parser = References(); parser.path = path; parser.feed(html)
    pid = re.search(r'Program ID:\s*<code[^>]*>([^<]+)</code>', html)
    keys = sorted(set(re.findall(r"localStorage\.(?:setItem|getItem)\(\s*['\"]([^'\"]+)['\"]", html)))
    assert pid and keys, f'Missing original ID/storage key: {path}'
    relative = path.relative_to(source)
    # Use the full relative filename so additional nested samples remain unique.
    ident = 'chess3-' + str(relative.with_suffix('')).replace('/', '-')
    catalog.append(dict(id=ident, group='Chess 3', name=str(relative), path=str(path.relative_to(repo)), kind='chess-sample', programId=pid.group(1), storageKey=keys[0], storageKeys=keys))
assert len({p['id'] for p in catalog}) == len(catalog)
assert len({p['programId'] for p in catalog}) == len(catalog)
assert len({p['storageKey'] for p in catalog}) == len(catalog)
(output/'catalog.json').write_text(json.dumps(catalog, indent=2)+'\n')
(output/'source-checksums.json').write_text(json.dumps(checksums, indent=2)+'\n')
shutil.copyfile(project/'host.js', output/'host.js')
print(f'{len(catalog)} programs discovered; {len(checksums)} original files copied unchanged')
